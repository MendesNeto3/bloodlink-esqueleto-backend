package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.adapter.out.persistence.DoacaoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoacaoUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.out.RegistrarDoacaoComando;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doacao;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DoacaoService implements DoacaoUseCase {

    private static final Logger log = LoggerFactory.getLogger(DoacaoService.class);
    private static final int VOLUME_MINIMO_ML = 300;
    private static final int VOLUME_MAXIMO_ML = 600;

    private final DoadorRepositoryPort doadores;
    private final DoacaoRepositoryPort doacoes;
    private final Clock clock;


    public DoacaoService (DoadorRepositoryPort doadorRepositoryPort,
                          DoacaoRepositoryPort doacaoRepositoryPort,
                          Clock clock) {
        this.clock = clock;
        this.doadores = doadorRepositoryPort;
        this.doacoes = doacaoRepositoryPort;
    }

    @Override
    public Doacao registrar(RegistrarDoacaoComando comando) {
        if (comando.doadorId() == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        if (comando.registradaPor() == null) {
            throw new IllegalArgumentException("Não foi possível identificar quem registrou a doação");
        }
        if (comando.data() == null) {
            throw new IllegalArgumentException("A data da doação é obrigatória");
        }
        if (comando.data().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("A data da doação não pode ser futura");
        }
        if (comando.volumeMl() < VOLUME_MINIMO_ML || comando.volumeMl() > VOLUME_MAXIMO_ML) {
            throw new IllegalArgumentException(
                    "O volume da doação deve estar entre " + VOLUME_MINIMO_ML + " e " + VOLUME_MAXIMO_ML + " mL");
        }

        try {
            Doador doador = doadores.buscarPorId(comando.doadorId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Doador", comando.doadorId()));
            if (!doador.ativo()) {
                throw new ConflitoException("Doador inativo não pode ter doação registrada");
            }
            if (doacoes.existePorDoadorEData(comando.doadorId(), comando.data())) {
                throw new ConflitoException("Já existe uma doação registrada para este doador nesta data");
            }
            Doacao doacao = new Doacao(UUID.randomUUID(), comando.doadorId(), comando.data(),
                    comando.volumeMl(), comando.registradaPor(), clock.instant());
            Doacao salva = doacoes.salvar(doacao);
            log.info("Doação {} registrada para o doador {}", salva.id(), comando.doadorId());
            return salva;
        } catch (RecursoNaoEncontradoException | ConflitoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao registrar doação do doador {} - causa: {}", comando.doadorId(), MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("registrar a doação");
        }
    }

    @Override
    public List<Doacao> historico(UUID doadorId) {
        if (doadorId == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        try {
            doadores.buscarPorId(doadorId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Doador", doadorId));
            return doacoes.listarPorDoador(doadorId);
        } catch (RecursoNaoEncontradoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao consultar histórico do doador {} - causa: {}", doadorId, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("consultar o histórico de doações");
        }
    }
}
