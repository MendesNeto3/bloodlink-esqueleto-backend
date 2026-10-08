package com.bloodlink.api.shared.tenant.domain;

import java.util.List;

public record PaginaProvisionamentos(
        List<TenantProvisionamento> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPagina
) {
}
