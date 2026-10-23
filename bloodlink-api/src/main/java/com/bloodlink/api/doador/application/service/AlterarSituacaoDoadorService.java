package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.AlterarSituacaoDoadorUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;

import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlterarSituacaoDoadorService implements AlterarSituacaoDoadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(AlterarSituacaoDoadorService.class);

    private final DoadorRepositoryPort repositoryPort;
    private final Clock clock;

    public AlterarSituacaoDoadorService(DoadorRepositoryPort repositoryPort, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.clock = clock;
    }

    @Override
    public Doador inativar(UUID id) {
        Doador atual = buscar(id, "inativar");
        if (!atual.ativo()) {
            log.info("Doador {} já estava inativo; nada foi gravado", id);
            return atual; // idempotente: não regrava nem mexe em atualizadoEm
        }
        return salvar(atual.inativado(clock.instant()), id, "inativar", "inativado");
    }

    @Override
    public Doador reativar(UUID id) {
        Doador atual = buscar(id, "reativar");
        if (atual.ativo()) {
            log.info("Doador {} já estava ativo; nada foi gravado", id);
            return atual;
        }
        return salvar(atual.reativado(clock.instant()), id, "reativar", "reativado");
    }

    private Doador buscar(UUID id, String operacao) {
        if (id == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        Optional<Doador> encontrado;
        try {
            encontrado = repositoryPort.buscarPorId(id);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o doador {} para {} - causa: {}", id, operacao, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException(operacao);
        }
        return encontrado.orElseThrow(() -> new RecursoNaoEncontradoException("Doador", id));
    }

    private Doador salvar(Doador alterado, UUID id, String operacao, String resultado) {
        try {
            Doador salvo = repositoryPort.salvar(alterado);
            log.info("Doador {} {}", id, resultado);
            return salvo;
        } catch (RuntimeException e) {
            log.error("Falha ao {} o doador {} - causa: {}", operacao, id, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException(operacao);
        }
    }
}
