package com.bloodlink.api.shared.tenant.adapter.in.web;

import com.bloodlink.api.shared.tenant.domain.ResultadoMigracao;

import java.util.Map;

public record MigracaoTenantsResponse (
        int total,
        Map<String, Integer> migrados,
        Map<String, String> falhas) {

    public static MigracaoTenantsResponse de(ResultadoMigracao r) {
        return new MigracaoTenantsResponse(r.total(), r.migrados(), r.falhas());
    }
}
