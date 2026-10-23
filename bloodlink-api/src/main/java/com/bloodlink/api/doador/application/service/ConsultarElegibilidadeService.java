package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.adapter.out.persistence.DoacaoRepositoryPort;
import com.bloodlink.api.doador.adapter.out.persistence.ImpedimentoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.ConsultarElegibilidadeUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.out.*;
import com.bloodlink.api.doador.application.port.out.ResultadoElegibilidade;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConsultarElegibilidadeService implements ConsultarElegibilidadeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultarElegibilidadeService.class);

    private final DoadorRepositoryPort doadores;
    private final DoacaoRepositoryPort doacoes;
    private final ImpedimentoRepositoryPort impedimentos;
    private final AvaliadorElegibilidade avaliador;

    public ConsultarElegibilidadeService(DoadorRepositoryPort doadores,
                                         DoacaoRepositoryPort doacoes,
                                         ImpedimentoRepositoryPort impedimentos,
                                         AvaliadorElegibilidade avaliador) {
        this.doadores = doadores;
        this.doacoes = doacoes;
        this.impedimentos = impedimentos;
        this.avaliador = avaliador;
    }

    @Override
    public ResultadoElegibilidade consultar(UUID doadorId) {
        if (doadorId == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        try {
            Doador doador = doadores.buscarPorId(doadorId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Doador", doadorId));
            List<LocalDate> datas = doacoes.datasPorDoadores(List.of(doadorId))
                    .getOrDefault(doadorId, List.of());
            List<Impedimento> imps = impedimentos.naoEncerradosPorDoadores(List.of(doadorId))
                    .getOrDefault(doadorId, List.of());
            return avaliador.avaliar(doador, datas, imps);
        } catch (RecursoNaoEncontradoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao consultar elegibilidade do doador {} - causa: {}", doadorId, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("consultar a elegibilidade");
        }
    }
}
