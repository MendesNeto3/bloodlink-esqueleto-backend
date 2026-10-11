package com.bloodlink.api.shared.tenant.application.port.out;

import com.bloodlink.api.shared.tenant.domain.ResultadoMigracao;

public interface MigrarTenantsUseCase {
    ResultadoMigracao migrarTodos();
}
