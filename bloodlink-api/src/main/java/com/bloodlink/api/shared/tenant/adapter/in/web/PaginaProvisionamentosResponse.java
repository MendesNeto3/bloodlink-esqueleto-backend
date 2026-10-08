package com.bloodlink.api.shared.tenant.adapter.in.web;

import com.bloodlink.api.shared.tenant.domain.PaginaProvisionamentos;

import java.util.List;

public record PaginaProvisionamentosResponse(
        List<ProvisionamentoResponse> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas){

    public static PaginaProvisionamentosResponse de(PaginaProvisionamentos p) {
        return new PaginaProvisionamentosResponse(
                p.conteudo().stream().map(ProvisionamentoResponse::de).toList(),
                p.pagina(), p.tamanho(), p.totalElementos(), p.totalPagina());
    }
}
