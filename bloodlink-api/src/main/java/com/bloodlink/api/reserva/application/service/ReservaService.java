package com.bloodlink.api.reserva.application.service;

import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.api.reserva.application.port.in.ReservaUseCase;
import com.bloodlink.api.reserva.application.port.out.CriarReservaComando;
import com.bloodlink.api.reserva.application.port.out.ReservaRepositoryPort;
import com.bloodlink.api.reserva.domain.Reserva;
import com.bloodlink.api.reserva.domain.ReservaComSituacao;
import com.bloodlink.api.reserva.domain.SituacaoReserva;
import com.bloodlink.api.reserva.exception.OperacaoReservaException;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class ReservaService implements ReservaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int UNIDADES_MAXIMO = 500;
    private static final int VALIDADE_MAXIMA_DIAS = 365;
    private static final int LIMITE_MAXIMO = 200;
    private static final int JANELA_DE_BUSCA = 500;

    private final ReservaRepositoryPort reservas;
    private final Clock clock;

    public ReservaService(ReservaRepositoryPort reservas, Clock clock) {
        this.reservas = reservas;
        this.clock = clock;
    }

    @Override
    public ReservaComSituacao criar(CriarReservaComando comando) {
        if (comando == null) {
            throw new IllegalArgumentException("Os dados da reserva são obrigatórios");
        }
        String hospital = validarHospital(comando.hospital());
        TipoSanguineo tipo = TipoSanguineo.de(comando.tipoSanguineo())
                .orElseThrow(() -> new IllegalArgumentException("Tipo sanguíneo inválido"));
        int unidades = validarUnidades(comando.unidades());
        LocalDate hoje = hoje();
        LocalDate validaAte = validarValidade(comando.validaAte(), hoje);
        if (comando.criadoPor() == null) {
            throw new IllegalArgumentException("Não foi possível identificar o usuário");
        }

        Reserva nova = Reserva.nova(hospital, tipo, unidades, validaAte, comando.criadoPor(), Instant.now(clock));
        try {
            Reserva salva = reservas.salvar(nova);
            log.info("Reserva {} registrada", salva.id());
            return new ReservaComSituacao(salva, salva.situacao(hoje));
        } catch (RuntimeException e) {
            log.error("Falha ao registrar a reserva {} - causa: {}", nova.id(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao registrar a reserva", e);
            throw new OperacaoReservaException("registrar a reserva", e);
        }
    }

    @Override
    public List<ReservaComSituacao> listar(SituacaoReserva situacao, int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e " + LIMITE_MAXIMO);
        }
        LocalDate hoje = hoje();
        try {
            // "vencida" depende da data de hoje, então o filtro é feito depois de ler as mais recentes
            return reservas.listarRecentes(JANELA_DE_BUSCA).stream()
                    .map(r -> new ReservaComSituacao(r, r.situacao(hoje)))
                    .filter(r -> situacao == null || r.situacao() == situacao)
                    .limit(limite)
                    .toList();
        } catch (RuntimeException e) {
            log.error("Falha ao listar as reservas - causa: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha ao listar as reservas", e);
            throw new OperacaoReservaException("listar as reservas", e);
        }
    }

    @Override
    @Transactional
    public ReservaComSituacao liberar(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("O id da reserva é obrigatório");
        }
        LocalDate hoje = hoje();
        try {
            Reserva atual = reservas.buscarPorId(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva", id));
            Reserva liberada = reservas.salvar(atual.liberar(hoje, Instant.now(clock)));
            log.info("Reserva {} liberada", id);
            return new ReservaComSituacao(liberada, liberada.situacao(hoje));
        } catch (RecursoNaoEncontradoException | ConflitoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao liberar a reserva {} - causa: {}", id, MascaraLog.causa(e));
            log.debug("Detalhe da falha ao liberar a reserva", e);
            throw new OperacaoReservaException("liberar a reserva", e);
        }
    }

    private LocalDate hoje() {
        return LocalDate.now(clock.withZone(FUSO));
    }

    private static String validarHospital(String hospital) {
        if (hospital == null || hospital.isBlank()) {
            throw new IllegalArgumentException("O hospital é obrigatório");
        }
        String limpo = hospital.trim().replaceAll("\\s+", " ");
        if (limpo.length() < 3 || limpo.length() > 150) {
            throw new IllegalArgumentException("O nome do hospital deve ter entre 3 e 150 caracteres");
        }
        return limpo;
    }

    private static int validarUnidades(Integer unidades) {
        if (unidades == null || unidades < 1 || unidades > UNIDADES_MAXIMO) {
            throw new IllegalArgumentException("As unidades devem estar entre 1 e " + UNIDADES_MAXIMO);
        }
        return unidades;
    }

    private static LocalDate validarValidade(LocalDate validaAte, LocalDate hoje) {
        if (validaAte == null) {
            throw new IllegalArgumentException("A data de validade é obrigatória");
        }
        if (validaAte.isBefore(hoje)) {
            throw new IllegalArgumentException("A validade não pode estar no passado");
        }
        if (validaAte.isAfter(hoje.plusDays(VALIDADE_MAXIMA_DIAS))) {
            throw new IllegalArgumentException("A validade pode ser de no máximo " + VALIDADE_MAXIMA_DIAS + " dias");
        }
        return validaAte;
    }
}
