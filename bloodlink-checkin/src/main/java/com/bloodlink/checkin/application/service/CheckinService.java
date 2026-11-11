package com.bloodlink.checkin.application.service;

import com.bloodlink.checkin.application.exception.ConflitoException;
import com.bloodlink.checkin.application.exception.OperacaoCheckinException;
import com.bloodlink.checkin.application.exception.RecursoNaoEncontradoException;
import com.bloodlink.checkin.application.exception.RegraDeNegocioException;
import com.bloodlink.checkin.application.port.in.CheckinUseCase;
import com.bloodlink.checkin.application.port.out.CheckinRepositoryPort;
import com.bloodlink.checkin.domain.AgendamentoParaCheckin;
import com.bloodlink.checkin.domain.Checkin;
import com.bloodlink.checkin.domain.ItemFila;
import com.bloodlink.checkin.domain.ResultadoTriagem;
import com.bloodlink.checkin.domain.ResumoFila;
import com.bloodlink.checkin.domain.SituacaoCheckin;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class CheckinService implements CheckinUseCase {

    private static final Logger log = LoggerFactory.getLogger(CheckinService.class);
    private static final int OBSERVACAO_MAXIMA = 500;
    private static final String CONFIRMADO = "CONFIRMADO";

    private final CheckinRepositoryPort checkins;
    private final Clock clock;

    public CheckinService(CheckinRepositoryPort checkins, Clock clock) {
        this.checkins = checkins;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Checkin registrarChegada(UUID agendamentoId, UUID registradoPor) {
        exigir(agendamentoId, "O id do agendamento é obrigatório");
        exigir(registradoPor, "Não foi possível identificar quem registrou a chegada");
        return executar("registrar o check-in", () -> {
            AgendamentoParaCheckin agendamento = checkins.buscarAgendamento(agendamentoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado"));
            if (!CONFIRMADO.equals(agendamento.status())) {
                throw new RegraDeNegocioException("Só é possível fazer check-in de um agendamento confirmado");
            }
            if (!agendamento.data().equals(LocalDate.now(clock))) {
                throw new RegraDeNegocioException("O check-in só pode ser feito no dia do agendamento");
            }
            Checkin novo = Checkin.novo(agendamento.id(), agendamento.doadorId(), registradoPor, clock.instant());
            if (!checkins.inserir(novo)) {
                throw new ConflitoException("O check-in deste agendamento já foi registrado");
            }
            log.info("Check-in {} registrado para o agendamento {}", novo.id(), agendamento.id());
            return novo;
        });
    }

    @Override
    @Transactional
    public Checkin registrarTriagem(UUID checkinId, String resultado, String observacao, UUID triadoPor) {
        exigir(checkinId, "O id do check-in é obrigatório");
        exigir(triadoPor, "Não foi possível identificar quem registrou a triagem");
        ResultadoTriagem parecer = ResultadoTriagem.de(resultado);
        String obs = observacao == null || observacao.isBlank() ? null : observacao.trim();
        if (obs != null && obs.length() > OBSERVACAO_MAXIMA) {
            throw new IllegalArgumentException("A observação excede o tamanho máximo de " + OBSERVACAO_MAXIMA + " caracteres");
        }
        if (parecer == ResultadoTriagem.INAPTA && obs == null) {
            throw new IllegalArgumentException("Informe o motivo da inaptidão");
        }
        return executar("registrar a triagem", () -> {
            Checkin atual = checkins.buscarPorIdParaAtualizar(checkinId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Check-in não encontrado"));
            if (atual.triado()) {
                throw new ConflitoException("A triagem deste check-in já foi registrada");
            }
            AgendamentoParaCheckin agendamento = checkins.buscarAgendamento(atual.agendamentoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado"));
            if (!CONFIRMADO.equals(agendamento.status())) {
                throw new RegraDeNegocioException(
                        "A triagem só pode ser registrada enquanto o agendamento estiver confirmado");
            }
            if (!agendamento.data().equals(LocalDate.now(clock))) {
                throw new RegraDeNegocioException("A triagem só pode ser registrada no dia do agendamento");
            }
            Checkin triado = atual.comTriagem(parecer, obs, triadoPor, clock.instant());
            if (!checkins.atualizarTriagem(triado)) {
                throw new ConflitoException("A triagem deste check-in já foi registrada");
            }
            log.info("Triagem do check-in {} registrada: {}", triado.id(), parecer);
            return triado;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Checkin buscar(UUID id) {
        exigir(id, "O id do check-in é obrigatório");
        return executar("consultar o check-in", () -> checkins.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Check-in não encontrado")));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoFila fila(LocalDate data, String situacao) {
        SituacaoCheckin filtro = situacao == null || situacao.isBlank() ? null : SituacaoCheckin.doFiltro(situacao);
        LocalDate dia = data != null ? data : LocalDate.now(clock);
        List<ItemFila> todos = executar("consultar a fila do check-in", () -> checkins.fila(dia));

        Map<SituacaoCheckin, Long> totais = new EnumMap<>(SituacaoCheckin.class);
        for (SituacaoCheckin s : SituacaoCheckin.values()) {
            totais.put(s, 0L);
        }
        todos.forEach(i -> totais.merge(i.situacao(), 1L, Long::sum));

        List<ItemFila> itens = filtro == null
                ? todos
                : todos.stream().filter(i -> i.situacao() == filtro).toList();
        return new ResumoFila(dia, totais, itens);
    }

    private static void exigir(Object valor, String mensagem) {
        if (valor == null) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private <T> T executar(String operacao, Supplier<T> acao) {
        try {
            return acao.get();
        } catch (RegraDeNegocioException | RecursoNaoEncontradoException | ConflitoException
                 | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao {} - causa: {}", operacao, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoCheckinException(operacao);
        }
    }
}