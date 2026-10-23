package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.adapter.out.persistence.FichaPreDoacaoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.in.FichaPreDoacaoUseCase;
import com.bloodlink.api.doador.application.port.out.SalvarFichaComando;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class FichaPreDoacaoService implements FichaPreDoacaoUseCase {

    private static final Logger log = LoggerFactory.getLogger(FichaPreDoacaoService.class);

    private final DoadorRepositoryPort doadores;
    private final FichaPreDoacaoRepositoryPort fichas;
    private final Clock clock;

    public FichaPreDoacaoService(DoadorRepositoryPort doadores, FichaPreDoacaoRepositoryPort fichas, Clock clock) {
        this.doadores = doadores;
        this.fichas = fichas;
        this.clock = clock;
    }

    @Override
    public FichaPreDoacao salvar(SalvarFichaComando comando) {
        if (comando == null || comando.doadorId() == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        FichaPreDoacao ficha = comando.paraFicha(Instant.now(clock));   // valida antes do try
        try {
            doadores.buscarPorId(comando.doadorId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Doador", comando.doadorId()));
            return fichas.salvar(ficha);
        } catch (RecursoNaoEncontradoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao salvar a ficha do doador {} - causa: {}", comando.doadorId(), MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("salvar a ficha de pré-doação");
        }
    }

    @Override
    public FichaPreDoacao consultar(UUID doadorId) {
        if (doadorId == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        try {
            return fichas.buscarPorDoador(doadorId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Ficha de pré-doação", doadorId));
        } catch (RecursoNaoEncontradoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao consultar a ficha do doador {} - causa: {}", doadorId, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("consultar a ficha de pré-doação");
        }
    }
}