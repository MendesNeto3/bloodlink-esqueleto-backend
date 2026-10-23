package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.AtualizarDoadorUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.out.AtualizarDoadorCommand;   // onde o arquivo realmente está
import com.bloodlink.api.doador.doadorException.AtualizacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
// RecursoNaoEncontradoException: copie o import do AlterarSituacaoDoadorService
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@Service
public class AtualizarDoadorService implements AtualizarDoadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(AtualizarDoadorService.class);

    private final DoadorRepositoryPort repositoryPort;
    private final Clock clock;

    public AtualizarDoadorService(DoadorRepositoryPort repositoryPort, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.clock = clock;
    }

    @Override
    public Doador atualizar(UUID id, AtualizarDoadorCommand comando) {
        if (id == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        if (comando == null) {
            throw new IllegalArgumentException("Dados do doador não informados");
        }

        Doador doadorAtual = buscar(id);
        if (doadorAtual == null) {
            throw new RecursoNaoEncontradoException("Doador", id);
        }

        String nome = ValidacoesDoador.validarNome(comando.nome());
        BigDecimal peso = ValidacoesDoador.validarPeso(comando.pesoKg());
        String telefone = ValidacoesDoador.validarTelefone(comando.telefone());
        String email = ValidacoesDoador.validarEmail(comando.email());

        boolean consentimento = comando.consentimentoResponsavel() != null
                ? comando.consentimentoResponsavel()
                : doadorAtual.consentimentoResponsavel();
        ValidacoesDoador.exigirConsentimentoSeAdolescente(doadorAtual.dataNascimento(), consentimento);

        if (semAlteracoes(doadorAtual, nome, telefone, email, peso, consentimento)) {
            log.info("Doador {} sem alterações; nada foi gravado", id);
            return doadorAtual;
        }

        Doador atualizado = doadorAtual.comDadosAtualizados(nome, telefone, email, peso, consentimento, clock.instant());
        garantirCamposImutaveis(doadorAtual, atualizado);

        try {
            Doador salvo = repositoryPort.salvar(atualizado);
            log.info("Doador {} atualizado", salvo.id());
            return salvo;
        } catch (RuntimeException e) {
            log.error("Falha ao atualizar o doador {} - causa: {}", id, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new AtualizacaoDoadorException(id, e);
        }
    }

    private Doador buscar(UUID id) {
        try {
            return repositoryPort.buscarPorId(id).orElse(null);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o doador {} para atualização - causa: {}", id, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new AtualizacaoDoadorException(id, e);
        }
    }

    private boolean semAlteracoes(Doador atual, String nome, String telefone, String email,
                                  BigDecimal peso, boolean consentimento) {
        return atual.nome().equals(nome)
                && Objects.equals(atual.telefone(), telefone)
                && Objects.equals(atual.email(), email)
                && atual.pesoKg().compareTo(peso) == 0
                && atual.consentimentoResponsavel() == consentimento;
    }

    /** Rede de segurança: CPF, nascimento, sexo, tipo sanguíneo e situação não podem mudar por aqui. */
    private void garantirCamposImutaveis(Doador atual, Doador atualizado) {
        boolean intacto = atual.id().equals(atualizado.id())
                && atual.cpf().equals(atualizado.cpf())
                && atual.dataNascimento().equals(atualizado.dataNascimento())
                && atual.sexo() == atualizado.sexo()
                && atual.tipoSanguineo() == atualizado.tipoSanguineo()
                && atual.ativo() == atualizado.ativo()
                && atual.criadoEm().equals(atualizado.criadoEm());
        if (!intacto) {
            // erro de programação, não do cliente: nunca deve acontecer
            throw new IllegalStateException("Atualização alteraria campos imutáveis do doador " + atual.id());
        }
    }
}