package com.bloodlink.estoque.adapter.in.web.dto;

/** motivo é opcional (TRANSFUSAO, DESCARTE ou OUTRO); sem ele a baixa vale como OUTRO. */
public record BaixaRequest(String tipoSanguineo, Integer quantidadeUnidades, String motivo) {
}
