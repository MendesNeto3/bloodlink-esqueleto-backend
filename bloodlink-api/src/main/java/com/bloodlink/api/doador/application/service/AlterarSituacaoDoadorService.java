package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.AlterarSituacaoDoadorUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.shared.Exception.RecursoNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
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
        if (id == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        Doador doador = buscar(id);
        if (!doador.ativo()) {
            return doador;
        }
        try {
            Doador salvo = repositoryPort.salvar(doador.inativado(Instant.now(clock)));
            log.info("Doador {} inativado", id);
            return salvo;
        } catch (RuntimeException ex) {
            log.error("Falha ao inativar o doador {}", id, ex);
            throw new OperacaoDoadorException("inativar", id, ex);
        }
    }

    @Override
    public Doador reativar(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        Doador doador = buscar(id);
        if (doador.ativo()) {
            return doador; // idempotente: já está ativo
        }
        try {
            Doador salvo = repositoryPort.salvar(doador.reativado(Instant.now(clock)));
            log.info("Doador {} reativado", id);
            return salvo;
        } catch (RuntimeException e) {
            log.error("Falha ao reativar o doador {}", id, e);
            throw new OperacaoDoadorException("reativar", id, e);
        }
    }

    private Doador buscar(UUID id) {
        return repositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doador", id));
    }
}
