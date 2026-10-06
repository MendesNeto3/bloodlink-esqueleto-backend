package com.bloodlink.api.shared.tenant.adapter.in.web;

import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;

import java.time.Instant;
import java.util.UUID;

public record ProvisionamentoResponse(
        UUID centroId,
        String schema,
        StatusProvisionamento status,
        Instant iniciadoEm,
        Instant atualizadoEm,
        Instant concluidoEm
) {
    public static ProvisionamentoResponse de(TenantProvisionamento p) {
        return new ProvisionamentoResponse(
                p.centroId(), p.schema(), p.status(),
                p.iniciadoEm(), p.atualizadoEm(), p.concluidoEm());
    }
}
