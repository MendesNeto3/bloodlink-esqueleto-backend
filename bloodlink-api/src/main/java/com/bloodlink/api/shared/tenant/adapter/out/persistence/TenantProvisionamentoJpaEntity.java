package com.bloodlink.api.shared.tenant.adapter.out.persistence;


import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_provisionamento")
public class TenantProvisionamentoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "centro_id", nullable = false)
    private UUID centroId;

    @Column(name = "schema", nullable = false, unique = true)
    private String schema;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusProvisionamento status;

    @Column(name = "iniciado_em", nullable = false)
    private Instant iniciadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    protected TenantProvisionamentoJpaEntity() {}

    public TenantProvisionamentoJpaEntity(UUID id, UUID centroId, String schema, StatusProvisionamento status,
                                          Instant iniciadoEm, Instant atualizadoEm, Instant concluidoEm) {
        this.id = id;
        this.centroId = centroId;
        this.schema = schema;
        this.status = status;
        this.iniciadoEm = iniciadoEm;
        this.atualizadoEm = atualizadoEm;
        this.concluidoEm = concluidoEm;
    }

    public UUID getId() { return id; }
    public UUID getCentroId() { return centroId; }
    public String getSchema() { return schema; }
    public StatusProvisionamento getStatus() { return status; }
    public Instant getIniciadoEm() { return iniciadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public Instant getConcluidoEm() { return concluidoEm; }
}
