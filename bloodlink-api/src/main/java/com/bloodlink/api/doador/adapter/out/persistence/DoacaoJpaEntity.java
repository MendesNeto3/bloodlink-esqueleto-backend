package com.bloodlink.api.doador.adapter.out.persistence;


import com.bloodlink.api.doador.domain.Doacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "doacao")
public class DoacaoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "doador_id", nullable = false, updatable = false)
    private UUID doadorId;

    @Column(nullable = false, updatable = false)
    private LocalDate data;

    @Column(name = "volume_ml", nullable = false)
    private int volumeMl;

    @Column(name = "registrada_por", nullable = false, updatable = false)
    private UUID registradaPor;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    protected DoacaoJpaEntity() {
    }

    public static DoacaoJpaEntity de(Doacao d) {
        DoacaoJpaEntity e = new DoacaoJpaEntity();
        e.id = d.id();
        e.doadorId = d.doadorId();
        e.data = d.data();
        e.volumeMl = d.volumeMl();
        e.registradaPor = d.registradaPor();
        e.criadaEm = d.criadaEm();
        return e;
    }

    public Doacao paraDominio() {
        return new Doacao(id, doadorId, data, volumeMl, registradaPor, criadaEm);
    }

    @Override
    public String toString() {
        return "DoacaoJpaEntity[id=" + id + "]";
    }
}
