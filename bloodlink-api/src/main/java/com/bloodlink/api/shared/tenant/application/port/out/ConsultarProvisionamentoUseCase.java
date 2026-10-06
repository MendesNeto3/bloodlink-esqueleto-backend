package com.bloodlink.api.shared.tenant.application.port.out;

import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;

public interface ConsultarProvisionamentoUseCase {

    TenantProvisionamento consultar(String schema);

}
