package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.AtualizarDoadorUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.out.AtualizarDoadorCommand;
import com.bloodlink.api.doador.doadorException.AtualizacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.shared.Exception.RecursoNaoEncontradoException;
import com.bloodlink.api.shared.Exception.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Service
public class AtualizarDoadorService implements AtualizarDoadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(AtualizarDoadorService.class);

    private final DoadorRepositoryPort repositoryPort;
    private final Clock clock;

    public AtualizarDoadorService (DoadorRepositoryPort repositoryPort, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.clock = clock;
    }

    @Override
    public Doador atualizar(UUID id, AtualizarDoadorCommand comando) {
        if (comando == null) {
            throw new IllegalArgumentException("Dados do doador não informados");
        }
        Doador doadorAtual = repositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doador", id));
        if (!doadorAtual.ativo()) {
            throw new RegraDeNegocioException("Doador inativo não pode ser atualizado; reative-o antes");
        }

        String nome = ValidacoesDoador.Validarnome(comando.nome());
        BigDecimal peso = ValidacoesDoador.validarPeso(comando.pesoKg());
        String telefone = ValidacoesDoador.validarTelefone(comando.telefone());
        String email = ValidacoesDoador.validarEmail(comando.email());
        boolean consentimento = Boolean.TRUE.equals(comando.consentimentoResponsavel());
        ValidacoesDoador.idadeEConsentimento(doadorAtual.dataNascimento(), consentimento, LocalDate.now(clock));

        if (semAlteracoes(doadorAtual, nome, telefone, email, peso, consentimento)) {
            log.info("Doador {} sem alterações; nada foi gravado", id);
            return doadorAtual; // não mexe em atualizadoEm nem escreve no banco
        }

        Doador atualizado = doadorAtual.comDadosAtualizados(nome, telefone, email, peso, consentimento, clock.instant());
        garantirCamposImutaveis(doadorAtual, atualizado);

        try {
            Doador salvo = repositoryPort.salvar(atualizado);
            log.info("Doador {} atualizado", salvo.id());
            return salvo;
        } catch (RuntimeException e) {
            log.error("Falha ao atualizar o doador {}", id, e);
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
