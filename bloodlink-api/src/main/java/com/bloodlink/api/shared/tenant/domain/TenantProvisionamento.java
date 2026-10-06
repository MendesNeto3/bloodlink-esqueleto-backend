package com.bloodlink.api.shared.tenant.domain;

import java.time.Instant;
import java.util.UUID;

public record TenantProvisionamento(
        UUID id,
        UUID centroId,
        String schema,
        StatusProvisionamento status,
        Instant iniciadoEm,
        Instant atualizadoEm,
        Instant concluidoEm
) {

    public TenantProvisionamento comStatus(StatusProvisionamento novoStatus, Instant agora) {
        return new TenantProvisionamento(
                id, centroId, schema, novoStatus, iniciadoEm, agora,
                novoStatus == StatusProvisionamento.CONCLUIDO ? agora : concluidoEm
        );
    }

    public TenantProvisionamento reiniciando(Instant agora) {
        return new TenantProvisionamento(
                id, centroId, schema, StatusProvisionamento.EM_ANDAMENTO, agora, agora, null
        );
    }
}
