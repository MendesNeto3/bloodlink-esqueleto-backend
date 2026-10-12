package com.bloodlink.api.doador.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
public record Doador(UUID id, String nome, String cpf, LocalDate dataNascimento, SexoBiologico sexo,
                     TipoSanguineo tipoSanguineo, BigDecimal pesoKg, String telefone, String email,
                     boolean consentimentoResponsavel, boolean ativo, Instant criadoEm, Instant atualizadoEm) {


    public int idadeEm(LocalDate data) {
        return Period.between(dataNascimento, data).getYears();
    }

    public Doador comDadosAtualizados(String nome, String telefone, String email, BigDecimal pesoKg,
                                      boolean consentimentoResponsavel, Instant agora) {
        return new Doador(id, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg, telefone, email,
                consentimentoResponsavel, ativo, criadoEm, agora);
    }

    public Doador inativado(Instant agora) {
        return new Doador(id, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg, telefone, email,
                consentimentoResponsavel, false, criadoEm, agora);
    }

    public Doador reativado(Instant agora) {
        return new Doador(id, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg, telefone, email,
                consentimentoResponsavel, true, criadoEm, agora);
    }
}