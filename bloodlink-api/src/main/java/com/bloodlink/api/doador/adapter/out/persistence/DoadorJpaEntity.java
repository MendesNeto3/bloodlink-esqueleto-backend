package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.OrigemCadastro;
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
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SexoBiologico sexo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sanguineo", nullable = false, length = 12)
    private TipoSanguineo tipoSanguineo;

    @Column(name = "peso_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @Column(length = 11)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Column(name = "consentimento_responsavel", nullable = false)
    private boolean consentimentoResponsavel;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(length = 80)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(name = "ultima_doacao_declarada")
    private LocalDate ultimaDoacaoDeclarada;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_cadastro", nullable = false, length = 20)
    private OrigemCadastro origemCadastro;

    @Column(name = "consentimento_lgpd_em")
    private Instant consentimentoLgpdEm;

    protected DoadorJpaEntity() {
    }

    public static DoadorJpaEntity de(Doador d) {
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
        e.cidade = d.cidade();
        e.uf = d.uf();
        e.ultimaDoacaoDeclarada = d.ultimaDoacaoDeclarada();
        e.origemCadastro = d.origemCadastro();
        e.consentimentoLgpdEm = d.consentimentoLgpdEm();
        return e;
    }

    public Doador paraDominio() {
        return new Doador(id, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg, telefone, email,
                consentimentoResponsavel, ativo, criadoEm, atualizadoEm,
                cidade, uf, ultimaDoacaoDeclarada, origemCadastro, consentimentoLgpdEm);
    }

    /** Dados pessoais nunca saem em log. */
    @Override
    public String toString() {
        return "DoadorJpaEntity[id=" + id + "]";
    }
}
