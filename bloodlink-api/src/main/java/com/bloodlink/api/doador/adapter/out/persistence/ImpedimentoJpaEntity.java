package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.application.port.out.Impedimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "impedimento")
public class ImpedimentoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "doador_id", nullable = false, updatable = false)
    private UUID doadorId;

    @Column(nullable = false, updatable = false)
    private boolean definitivo;

    @Column(nullable = false, updatable = false)
    private LocalDate inicio;

    @Column(updatable = false)
    private LocalDate fim;

    @Column(nullable = false, length = 500, updatable = false)
    private String motivo;

    @Column(name = "registrado_por", nullable = false, updatable = false)
    private UUID registradoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    protected ImpedimentoJpaEntity() {
    }

    public static ImpedimentoJpaEntity de(Impedimento i) {
        ImpedimentoJpaEntity e = new ImpedimentoJpaEntity();
        e.id = i.id();
        e.doadorId = i.doadorId();
        e.definitivo = i.definitivo();
        e.inicio = i.inicio();
        e.fim = i.fim();
        e.motivo = i.motivo();
        e.registradoPor = i.registradoPor();
        e.criadoEm = i.criadoEm();
        e.encerradoEm = i.encerradoEm();
        return e;
    }

    public Impedimento paraDominio() {
        return new Impedimento(id, doadorId, definitivo, inicio, fim, motivo, registradoPor, criadoEm, encerradoEm);
    }

    /** O motivo é dado de saúde: nunca sai em log. */
    @Override
    public String toString() {
        return "ImpedimentoJpaEntity[id=" + id + "]";
    }
}