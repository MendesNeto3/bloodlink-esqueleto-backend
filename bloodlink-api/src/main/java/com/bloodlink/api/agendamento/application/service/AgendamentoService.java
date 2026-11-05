package com.bloodlink.api.agendamento.application.service;

import com.bloodlink.api.agendamento.application.port.in.AgendamentoRepositoryPort;
import com.bloodlink.api.agendamento.application.usecase.AgendamentoUseCase;
import com.bloodlink.api.agendamento.domain.Agendamento;
import com.bloodlink.api.agendamento.domain.FiltroAgendamento;
import com.bloodlink.api.agendamento.domain.SolicitarAgendamentoComando;
import com.bloodlink.api.doador.adapter.out.persistence.DoacaoRepositoryPort;
import com.bloodlink.api.doador.adapter.out.persistence.ImpedimentoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.out.Impedimento;
import com.bloodlink.api.doador.application.port.out.ResultadoElegibilidade;
import com.bloodlink.api.doador.application.service.AvaliadorElegibilidade;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.StatusElegibilidade;
import com.bloodlink.api.shared.log.MascaraLog;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class AgendamentoService implements AgendamentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoService.class);
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int OBSERVACAO_MAXIMA = 500;
    private static final int MOTIVO_MAXIMO = 300;
    private static final int LIMITE_MAXIMO = 200;

    private final AgendamentoRepositoryPort agendamentos;
    private final DoadorRepositoryPort doadores;
    private final DoacaoRepositoryPort doacoes;
    private final ImpedimentoRepositoryPort impedimentos;
    private final AvaliadorElegibilidade avaliador;
    private final ConfiguracaoAgendamento configuracao;
    private final Clock clock;

    public AgendamentoService(AgendamentoRepositoryPort agendamentos,
                              DoadorRepositoryPort doadores,
                              DoacaoRepositoryPort doacoes,
                              ImpedimentoRepositoryPort impedimentos,
                              AvaliadorElegibilidade avaliador,
                              ConfiguracaoAgendamento configuracao,
                              Clock clock) {
        this.agendamentos = agendamentos;
        this.doadores = doadores;
        this.doacoes = doacoes;
        this.impedimentos = impedimentos;
        this.avaliador = avaliador;
        this.configuracao = configuracao;
        this.clock = clock;
    }

    @Override
    public Agendamento solicitar(SolicitarAgendamentoComando comando) {
        if (comando == null || comando.doadorId() == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        if (comando.solicitadoPor() == null || comando.origem() == null) {
            throw new IllegalArgumentException("Não foi possível identificar quem solicitou o agendamento");
        }
        if (comando.data() == null) {
            throw new IllegalArgumentException("A data do agendamento é obrigatória");
        }
        if (comando.hora() == null) {
            throw new IllegalArgumentException("O horário do agendamento é obrigatório");
        }
        LocalTime hora = comando.hora().truncatedTo(ChronoUnit.MINUTES);
        String observacao = texto(comando.observacao(), OBSERVACAO_MAXIMA, "A observação");
        configuracao.validarData(comando.data());
        if (!configuracao.atende(comando.data())) {
            throw new IllegalArgumentException("O hemocentro não atende neste dia da semana");
        }
        if (!configuracao.horarios().contains(hora)) {
            throw new IllegalArgumentException("Horário fora da grade de atendimento");
        }

        return executar("solicitar o agendamento", comando.doadorId(), () -> {
            Doador doador = doadores.buscarPorId(comando.doadorId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Doador", comando.doadorId()));
            if (!doador.ativo()) {
                throw new ConflitoException("Doador inativo não pode ter agendamento");
            }
            if (agendamentos.buscarAtivoDoDoador(comando.doadorId()).isPresent()) {
                throw new ConflitoException("Este doador já tem um agendamento ativo");
            }

            List<LocalDate> datas = doacoes.datasPorDoadores(List.of(comando.doadorId()))
                    .getOrDefault(comando.doadorId(), List.of());
            List<Impedimento> imps = impedimentos.naoEncerradosPorDoadores(List.of(comando.doadorId()))
                    .getOrDefault(comando.doadorId(), List.of());
            ResultadoElegibilidade resultado = avaliador.avaliarEm(doador, datas, imps, comando.data());
            if (resultado.status() != StatusElegibilidade.ELEGIVEL) {
                throw new RegraDeNegocioException(mensagemInelegivel(resultado));
            }
            if (agendamentos.contarAtivosNoHorario(comando.data(), hora) >= configuracao.vagasPorHorario()) {
                throw new ConflitoException("Este horário não tem mais vagas");
            }

            Agendamento salvo = agendamentos.salvar(Agendamento.novo(
                    comando.doadorId(), comando.data(),hora, comando.origem(),
                    observacao, comando .solicitadoPor(), clock.instant()));
            log.info("Agendamento {} solicitado para o doador {}", salvo.id(), comando.doadorId());
            return salvo;
        });
    }

    @Override
    public List<Agendamento> listarDoDoador(UUID doadorId) {
        exigir(doadorId, "O id do doador é obrigatório");
        return executar("consultar os agendamentos", doadorId, () -> agendamentos.listarPorDoador(doadorId));
    }

    @Override
    public Optional<Agendamento> ativoDoDoador(UUID doadorId) {
        exigir(doadorId, "O id do doador é obrigatório");
        return executar("consultar o agendamento ativo", doadorId, () -> agendamentos.buscarAtivoDoDoador(doadorId));
    }

    @Override
    public Agendamento cancelarDoDoador(UUID doadorId, UUID agendamentoId, String motivo) {
        exigir(doadorId, "O id do doador é obrigatório");
        exigir(agendamentoId, "O id do agendamento é obrigatório");
        String motivoValidado = texto(motivo, MOTIVO_MAXIMO, "O motivo");
        return executar("cancelar o agendamento", agendamentoId, () -> {
            Agendamento atual = agendamentos.buscarPorId(agendamentoId)
                    .filter(a -> a.doadorId().equals(doadorId))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", agendamentoId));
            Agendamento cancelado = agendamentos.salvar(atual.cancelar(motivoValidado, clock.instant()));
            log.info("Agendamento {} cancelado pelo doador", cancelado.id());
            return cancelado;
        });
    }

    @Override
    public Agendamento buscar(UUID agendamentoId) {
        exigir(agendamentoId, "O id do agendamento é obrigatório");
        return executar("consultar o agendamento", agendamentoId, () -> agendamentos.buscarPorId(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", agendamentoId)));
    }

    @Override
    public List<Agendamento> listar(FiltroAgendamento filtro, int limite) {
        if (filtro == null) {
            throw new IllegalArgumentException("O filtro é obrigatório");
        }
        if (filtro.de() != null && filtro.ate() != null && filtro.de().isAfter(filtro.ate())) {
            throw new IllegalArgumentException("A data inicial não pode ser depois da final");
        }
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e " + LIMITE_MAXIMO);
        }
        return executar("listar os agendamentos", null, () -> agendamentos.listar(filtro, limite));
    }

    @Override
    public Agendamento confirmar(UUID agendamentoId) {
        exigir(agendamentoId, "O id do agendamento é obrigatório");
        return executar("confirmar o agendamento", agendamentoId, () -> {
            Agendamento atual = agendamentos.buscarPorId(agendamentoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", agendamentoId));
            Agendamento confirmado = agendamentos.salvar(atual.confirmar(clock.instant()));
            log.info("Agendamento {} confirmado", confirmado.id());
            return confirmado;
        });
    }

    @Override
    public Agendamento cancelarPelaEquipe(UUID agendamentoId, String motivo) {
        exigir(agendamentoId, "O id do agendamento é obrigatório");
        String motivoValidado = texto(motivo, MOTIVO_MAXIMO, "O motivo");
        return executar("cancelar o agendamento", agendamentoId, () -> {
            Agendamento atual = agendamentos.buscarPorId(agendamentoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", agendamentoId));
            Agendamento cancelado = agendamentos.salvar(atual.cancelar(motivoValidado, clock.instant()));
            log.info("Agendamento {} cancelado pela equipe", cancelado.id());
            return cancelado;
        });
    }

    private <T> T executar(String operacao, UUID referencia, Supplier<T> acao) {
        try {
            return acao.get();
        } catch (RecursoNaoEncontradoException | ConflitoException | RegraDeNegocioException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao {} (ref {}) - causa: {}", operacao, referencia, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException(operacao);
        }
    }

    private static void exigir(Object valor, String mensagem) {
        if (valor == null) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private static String texto(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.trim();
        if (limpo.length() > maximo) {
            throw new IllegalArgumentException(campo + " excede o tamanho máximo de " + maximo + " caracteres");
        }
        return limpo;
    }

    private static String mensagemInelegivel(ResultadoElegibilidade resultado) {
        if (resultado.status() == StatusElegibilidade.AGUARDANDO && resultado.elegivelEm() != null) {
            return "O doador só poderá doar a partir de " + resultado.elegivelEm().format(DATA_BR);
        }
        return "O doador não está elegível para doar na data escolhida";
    }
}
