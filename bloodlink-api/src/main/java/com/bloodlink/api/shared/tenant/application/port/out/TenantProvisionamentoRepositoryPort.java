package com.bloodlink.api.shared.tenant.application.port.out;

import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TenantProvisionamentoRepositoryPort {
    TenantProvisionamento salvar(TenantProvisionamento provisionamento);
    Optional<TenantProvisionamento> buscarPorSchema(String schema);
    List<TenantProvisionamento> listarPorStatusEIniciadoAntesDe(StatusProvisionamento status, Instant limite);
}
