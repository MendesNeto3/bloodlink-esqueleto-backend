package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.in.RegistrarDoadorUseCase;
import com.bloodlink.api.doador.application.port.out.RegistrarDoadorCommand;
import com.bloodlink.api.doador.doadorException.RegistroDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.SexoBiologico;
import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.api.shared.Conversor.ConversorEnum;
import com.bloodlink.api.shared.Exception.ConflitoException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import java.util.UUID;

@Service
public class RegistrarDoadorService implements RegistrarDoadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegistrarDoadorService.class);

    private final DoadorRepositoryPort repositoryPort;
    private final Clock clock;

    public RegistrarDoadorService(final DoadorRepositoryPort repositoryPort, final Clock clock) {
        this.repositoryPort = repositoryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Doador registrar(RegistrarDoadorCommand comando) {
        if (comando == null) {
            throw new IllegalArgumentException("Dados do doador não informados");
        }

        LocalDate hoje = LocalDate.now(clock);
        Instant agora = clock.instant();

        String nome = ValidacoesDoador.Validarnome(comando.nome());
        String cpf = ValidacoesDoador.validarCpf(comando.cpf());
        LocalDate nascimento = ValidacoesDoador.validarDataNascimento(comando.dataNascimento(), hoje);
        SexoBiologico sexo = ConversorEnum.obrigatorio(SexoBiologico.class, comando.sexo(), "sexo");
        TipoSanguineo tipo = ConversorEnum.obrigatorio(TipoSanguineo.class, comando.tipoSanguineo(), "tipoSanguineo");
        BigDecimal peso = ValidacoesDoador.validarPeso(comando.pesoKg());
        String telefone = ValidacoesDoador.validarTelefone(comando.telefone());
        String email = ValidacoesDoador.validarEmail(comando.email());
        boolean consentimento = Boolean.TRUE.equals(comando.consentimentoResponsavel());
        ValidacoesDoador.idadeEConsentimento(nascimento, consentimento, hoje);

        UUID doadorId = UUID.randomUUID();
        Doador novoDoador = new Doador(doadorId, nome, cpf, nascimento, sexo, tipo, peso,
                telefone, email, consentimento, true, agora, agora);

        try {
            garantirCpfDisponivel(cpf);
            Doador doadorSalvo = repositoryPort.salvar(novoDoador);
            log.info("Doador {} registrado", doadorSalvo.id()); // não loga CPF (dado pessoal, LGPD)
            return doadorSalvo;
        } catch (ConflitoException e) {
            throw e; // já tem significado de negócio (409): não embrulhar
        } catch (RuntimeException e) {
            log.error("Falha ao registrar o doador {}", doadorId, e);
            throw new RegistroDoadorException(doadorId, e);
        }
    }

    private void garantirCpfDisponivel(String cpf) {
        if (repositoryPort.existePorCpf(cpf)) {
            throw new ConflitoException("Já existe um doador cadastrado com este CPF");
        }
    }
}
