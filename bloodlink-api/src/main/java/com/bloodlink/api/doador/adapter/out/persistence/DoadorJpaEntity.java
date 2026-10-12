package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.SexoBiologico;
import com.bloodlink.api.doador.domain.TipoSanguineo;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "doador")
public class DoadorJpaEntity {

    @Id
    UUID id;

    @Column(nullable = false, length = 120)
    String nome;

    @Column(nullable = false, length = 11)
    String cpf;

    @Column(name = "data_nascimento", nullable = false)
    LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    SexoBiologico sexo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sanguineo", nullable = false, length = 12)
    TipoSanguineo tipoSanguineo;

    @Column(name = "peso_kg", nullable = false, precision = 5, scale = 2)
    BigDecimal pesoKg;

    @Column(length = 11)
    String telefone;

    @Column(length = 120)
    String email;

    @Column(name = "consentimento_responsavel", nullable = false)
    boolean consentimentoResponsavel;

    @Column(nullable = false)
    boolean ativo;

    @Column(name = "criado_em", nullable = false)
    Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    Instant atualizadoEm;

    protected DoadorJpaEntity() {}

    static DoadorJpaEntity de(Doador d) {
        DoadorJpaEntity e = new DoadorJpaEntity();
        e.id = d.id();
        e.nome = d.nome();
        e.cpf = d.cpf();
        e.dataNascimento = d.dataNascimento();
        e.sexo = d.sexo();
        e.tipoSanguineo = d.tipoSanguineo();
        e.pesoKg = d.pesoKg();
        e.telefone = d.telefone();
        e.email = d.email();
        e.consentimentoResponsavel = d.consentimentoResponsavel();
        e.ativo = d.ativo();
        e.criadoEm = d.criadoEm();
        e.atualizadoEm = d.atualizadoEm();
        return e;
    }

    Doador paraDominio() {
        return new Doador(id, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg, telefone, email,
                consentimentoResponsavel, ativo, criadoEm, atualizadoEm);
    }
}

