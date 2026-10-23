package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.adapter.out.persistence.ImpedimentoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.in.ImpedimentoUseCase;
import com.bloodlink.api.doador.application.port.out.Impedimento;
import com.bloodlink.api.doador.application.port.out.RegistrarImpedimentoComando;   // onde o arquivo realmente está
import com.bloodlink.api.doador.doadorException.DoadorNaoEncontradoException;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class ImpedimentoService implements ImpedimentoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ImpedimentoService.class);

    private static final int MOTIVO_MAXIMO = 500;

    private final DoadorRepositoryPort doadores;
    private final ImpedimentoRepositoryPort impedimentos;
    private final Clock clock;

    public ImpedimentoService(DoadorRepositoryPort doadores, ImpedimentoRepositoryPort impedimentos,
                              Clock clock) {
        this.doadores = doadores;
        this.impedimentos = impedimentos;
        this.clock = clock;
    }

    @Override
    public Impedimento registrar(RegistrarImpedimentoComando comando) {
        if (comando.registradoPor() == null) {
            throw new RegraDeNegocioException("Não foi possível identificar quem registrou o impedimento");
        }
        if (comando.inicio() == null) {
            throw new RegraDeNegocioException("O início do impedimento é obrigatório");
        }
        if (comando.motivo() == null || comando.motivo().isBlank()) {
            throw new RegraDeNegocioException("O motivo do impedimento é obrigatório");
        }
        String motivo = comando.motivo().trim();
        if (motivo.length() > MOTIVO_MAXIMO) {
            throw new RegraDeNegocioException("O motivo deve ter no máximo " + MOTIVO_MAXIMO + " caracteres");
        }
        if (comando.definitivo() && comando.fim() != null) {
            throw new RegraDeNegocioException("Impedimento definitivo não tem data final");
        }
        if (!comando.definitivo() && comando.fim() == null) {
            throw new RegraDeNegocioException("Impedimento temporário exige a data final");
        }
        if (!comando.definitivo() && comando.fim().isBefore(comando.inicio())) {
            throw new RegraDeNegocioException("A data final não pode ser anterior ao início");
        }

        try {
            doadores.buscarPorId(comando.doadorId())
                    .orElseThrow(() -> new DoadorNaoEncontradoException(comando.doadorId()));

            Impedimento impedimento = new Impedimento(UUID.randomUUID(), comando.doadorId(),
                    comando.definitivo(), comando.inicio(), comando.fim(), motivo,
                    comando.registradoPor(), clock.instant(), null);
            return impedimentos.salvar(impedimento);
        } catch (DoadorNaoEncontradoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao registrar impedimento do doador {}: {}", comando.doadorId(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao registrar impedimento", e);
            throw new OperacaoDoadorException("Não foi possível registrar o impedimento no momento");
        }
    }

    @Override
    public List<Impedimento> listar(UUID doadorId) {
        try {
            doadores.buscarPorId(doadorId)
                    .orElseThrow(() -> new DoadorNaoEncontradoException(doadorId));
            return impedimentos.listarPorDoador(doadorId);
        } catch (DoadorNaoEncontradoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao listar impedimentos do doador {}: {}", doadorId, MascaraLog.causa(e));
            log.debug("Detalhe da falha ao listar impedimentos", e);
            throw new OperacaoDoadorException("Não foi possível consultar os impedimentos no momento");
        }
    }

    @Override
    public Impedimento encerrar(UUID doadorId, UUID impedimentoId) {
        try {
            Impedimento impedimento = impedimentos.buscarPorId(impedimentoId)
                    // o impedimento tem que ser do doador da URL, senão responde 404 (não vaza que existe)
                    .filter(i -> i.doadorId().equals(doadorId))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Impedimento não encontrado"));
            if (impedimento.encerradoEm() != null) {
                throw new ConflitoException("Este impedimento já foi encerrado");
            }
            return impedimentos.salvar(impedimento.encerrado(clock.instant()));
        } catch (RecursoNaoEncontradoException | ConflitoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao encerrar impedimento {} do doador {}: {}",
                    impedimentoId, doadorId, MascaraLog.causa(e));
            log.debug("Detalhe da falha ao encerrar impedimento", e);
            throw new OperacaoDoadorException("Não foi possível encerrar o impedimento no momento");
        }
    }
}
