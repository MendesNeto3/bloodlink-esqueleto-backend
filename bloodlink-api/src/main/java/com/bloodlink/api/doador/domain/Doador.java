package com.bloodlink.api.doador.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

public record Doador(UUID id, String nome, String cpf, LocalDate dataNascimento, SexoBiologico sexo,
                     TipoSanguineo tipoSanguineo, BigDecimal pesoKg, String telefone, String email,
                     boolean consentimentoResponsavel, boolean ativo, Instant criadoEm, Instant atualizadoEm,
                     String cidade, String uf, LocalDate ultimaDoacaoDeclarada,
                     OrigemCadastro origemCadastro, Instant consentimentoLgpdEm) {

    /** Cadastro feito pela equipe, sem os dados do autocadastro. */
    public Doador(UUID id, String nome, String cpf, LocalDate dataNascimento, SexoBiologico sexo,
                  TipoSanguineo tipoSanguineo, BigDecimal pesoKg, String telefone, String email,
                  boolean consentimentoResponsavel, boolean ativo, Instant criadoEm, Instant atualizadoEm) {
        this(id, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg, telefone, email,
                consentimentoResponsavel, ativo, criadoEm, atualizadoEm,
                null, null, null, OrigemCadastro.EQUIPE, null);
    }

    public int idadeEm(LocalDate data) {
        return Period.between(dataNascimento, data).getYears();
    }

    public Doador comDadosAtualizados(String nome, String telefone, String email, BigDecimal pesoKg,
                                      boolean consentimentoResponsavel, Instant agora) {
        return new Doador(id, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg, telefone, email,
                consentimentoResponsavel, ativo, criadoEm, agora,
                cidade, uf, ultimaDoacaoDeclarada, origemCadastro, consentimentoLgpdEm);
    }

    public Doador inativado(Instant agora) {
        return new Doador(id, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg, telefone, email,
                consentimentoResponsavel, false, criadoEm, agora,
                cidade, uf, ultimaDoacaoDeclarada, origemCadastro, consentimentoLgpdEm);
    }

    public Doador reativado(Instant agora) {
        return new Doador(id, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg, telefone, email,
                consentimentoResponsavel, true, criadoEm, agora,
                cidade, uf, ultimaDoacaoDeclarada, origemCadastro, consentimentoLgpdEm);
    }

    public Doador comTipoSanguineo(TipoSanguineo novoTipo, Instant agora) {
        return new Doador(id, nome, cpf, dataNascimento, sexo, novoTipo, pesoKg, telefone, email,
                consentimentoResponsavel, ativo, criadoEm, agora,
                cidade, uf, ultimaDoacaoDeclarada, origemCadastro, consentimentoLgpdEm);
    }

    public Doador comPerfil(String telefone, String cidade, String uf, BigDecimal pesoKg, Instant agora) {
        return new Doador(id, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg, telefone, email,
                consentimentoResponsavel, ativo, criadoEm, agora,
                cidade, uf, ultimaDoacaoDeclarada, origemCadastro, consentimentoLgpdEm);
    }

    /** Nome, CPF e e-mail nunca saem em log. */
    @Override
    public String toString() {
        return "Doador[id=" + id + "]";
    }
}