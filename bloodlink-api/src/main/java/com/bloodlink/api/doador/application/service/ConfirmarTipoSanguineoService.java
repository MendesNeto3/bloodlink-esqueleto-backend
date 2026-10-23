package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.ConfirmarTipoSanguineoUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.common.log.MascaraLog;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class ConfirmarTipoSanguineoService implements ConfirmarTipoSanguineoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmarTipoSanguineoService.class);

    private final DoadorRepositoryPort doadores;
    private final Clock clock;

    public ConfirmarTipoSanguineoService(DoadorRepositoryPort doadores, Clock clock) {
        this.doadores = doadores;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Doador confirmar(UUID doadorId, TipoSanguineo tipo) {
        if (doadorId == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo sanguíneo é obrigatório");
        }
        try {
            Doador doador = doadores.buscarPorId(doadorId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Doador", doadorId));
            if (doador.tipoSanguineo() == tipo) {
                log.info("Tipo sanguíneo do doador {} já estava confirmado; nada foi gravado", doadorId);
                return doador;
            }
            Doador salvo = doadores.salvar(doador.comTipoSanguineo(tipo, clock.instant()));
            log.info("Tipo sanguíneo do doador {} atualizado", doadorId); // o tipo é dado de saúde: não vai no log
            return salvo;
        } catch (RecursoNaoEncontradoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao atualizar tipo sanguíneo do doador {} - causa: {}", doadorId, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("atualizar o tipo sanguíneo");
        }
    }
}
