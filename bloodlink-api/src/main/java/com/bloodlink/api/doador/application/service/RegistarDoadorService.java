package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.adapter.out.persistence.DoacaoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.in.RegistrarDoadorUseCase;
import com.bloodlink.api.doador.application.port.out.RegistrarDoadorCommand;
import com.bloodlink.api.doador.doadorException.DoadorNaoEncontradoException;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.doadorException.RegistroDoadorException;
import com.bloodlink.api.doador.domain.Doacao;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.SexoBiologico;
import com.bloodlink.api.doador.domain.TipoSanguineo;

import com.bloodlink.auth.shared.ConversorEnum.ConversorEnum;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class RegistarDoadorService implements RegistrarDoadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegistarDoadorService.class);

    private static final String CPF_DUPLICADO = "Já existe um doador cadastrado com este CPF";
    private static final String CONFLITO = "Os dados informados conflitam com um cadastro existente";

    private final Clock clock;
    private final DoacaoRepositoryPort doacoes;
    private final DoadorRepositoryPort repository;

    public RegistarDoadorService(DoadorRepositoryPort doadorRepositoryPort,
                                 DoacaoRepositoryPort doacaoRepositoryPort,
                                 Clock clock) {
        this.repository = doadorRepositoryPort;
        this.doacoes = doacaoRepositoryPort;
        this.clock = clock;
    }

    @Override
    public Doador registrar(RegistrarDoadorCommand comando) {
        if (comando == null) {
            throw new IllegalArgumentException("Os dados do doador são obrigatórios");
        }
        String nome = ValidacoesDoador.validarNome(comando.nome());
        String cpf = ValidacoesDoador.validarCpf(comando.cpf());
        LocalDate nascimento = ValidacoesDoador.validarNascimento(comando.dataNascimento());
        SexoBiologico sexo = ConversorEnum.obrigatorio(SexoBiologico.class, "sexo", comando.sexo());
        TipoSanguineo tipo = ConversorEnum.obrigatorio(TipoSanguineo.class, "tipoSanguineo", comando.tipoSanguineo());
        BigDecimal peso = ValidacoesDoador.validarPeso(comando.pesoKg());
        String telefone = ValidacoesDoador.validarTelefone(comando.telefone());
        String email = ValidacoesDoador.validarEmail(comando.email());
        boolean consentimento = Boolean.TRUE.equals(comando.consentimentoResponsavel());
        ValidacoesDoador.exigirConsentimentoSeAdolescente(nascimento, consentimento);

        Instant agora = clock.instant();
        Doador novo = new Doador(UUID.randomUUID(), nome, cpf, nascimento, sexo, tipo, peso,
                telefone, email, consentimento, true, agora, agora);

        try {
            if (repository.existePorCpf(cpf)) {
                throw new ConflitoException(CPF_DUPLICADO);
            }
            Doador salvo = repository.salvar(novo);
            log.info("Doador {} registrado", salvo.id());
            return salvo;
        } catch (ConflitoException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.warn("Cadastro do doador {} barrado por restrição do banco - causa: {}",
                    novo.id(), MascaraLog.causa(e));
            log.debug("Detalhe do conflito", e);
            throw new ConflitoException(CONFLITO);
        } catch (RuntimeException e) {
            log.error("Falha ao registrar o doador {} - causa: {}", novo.id(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao registrar o doador", e);
            throw new RegistroDoadorException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doacao> historico(UUID doadorId) {
        try {
            repository.buscarPorId(doadorId)
                    .orElseThrow(() -> new DoadorNaoEncontradoException(doadorId));
            return doacoes.listarPorDoador(doadorId);
        } catch (DoadorNaoEncontradoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao consultar histórico do doador {}: {}", doadorId, MascaraLog.causa(e));
            log.debug("Detalhe da falha ao consultar histórico", e);
            throw new OperacaoDoadorException("Não foi possível consultar o histórico no momento");
        }
    }
}