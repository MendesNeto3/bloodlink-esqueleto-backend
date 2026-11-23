package com.bloodlink.api.reserva.adapter.out.persistence;

import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.api.reserva.domain.Reserva;
import com.bloodlink.api.reserva.domain.StatusReserva;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reserva")
public class ReservaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "hospital_nome", nullable = false, length = 150)
    private String hospital;

    /** Guarda o rótulo ("O-", "AB+"): a coluna do modelo antigo tem 3 caracteres. */
    @Column(name = "tipo_sanguineo", nullable = false, length = 3)
    private String tipoSanguineo;

    @Column(name = "quantidade_unidades", nullable = false)
    private int unidades;

    @Column(name = "valida_ate", nullable = false)
    private LocalDate validaAte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReserva status;

    @Column(name = "criado_por", updatable = false)
    private UUID criadoPor;

    @Column(name = "solicitado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "liberada_em")
    private Instant liberadaEm;

    protected ReservaJpaEntity() {
    }

    public static ReservaJpaEntity de(Reserva r) {
        ReservaJpaEntity e = new ReservaJpaEntity();
        e.id = r.id();
        e.hospital = r.hospital();
        e.tipoSanguineo = r.tipoSanguineo().rotulo();
        e.unidades = r.unidades();
        e.validaAte = r.validaAte();
        e.status = r.status();
        e.criadoPor = r.criadoPor();
        e.criadoEm = r.criadoEm();
        e.liberadaEm = r.liberadaEm();
        return e;
    }

    public Reserva paraDominio() {
        TipoSanguineo tipo = TipoSanguineo.de(tipoSanguineo)
                .orElseThrow(() -> new IllegalStateException("Tipo sanguíneo inválido na reserva " + id));
        return new Reserva(id, hospital, tipo, unidades, validaAte, status, criadoPor, criadoEm, liberadaEm);
    }

    @Override
    public String toString() {
        return "ReservaJpaEntity[id=" + id + "]";
    }
}
