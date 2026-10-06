package com.bloodlink.api.shared.tenant.application.port.out;

import java.util.UUID;

public interface ProvisionarTenantUseCase {

    void provisionar(UUID centroId, String schema, boolean ativo);


}
