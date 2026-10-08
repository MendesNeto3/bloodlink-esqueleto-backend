package com.bloodlink.api.shared.tenant.application.port.out;


import com.bloodlink.api.shared.tenant.domain.PaginaProvisionamentos;

public interface ListarProvisionamentosUseCase {
    PaginaProvisionamentos listar(String status, int pagina, int tamanho);
}
