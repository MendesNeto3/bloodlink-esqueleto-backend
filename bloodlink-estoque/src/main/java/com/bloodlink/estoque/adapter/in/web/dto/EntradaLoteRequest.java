package com.bloodlink.estoque.adapter.in.web.dto;

import java.time.LocalDate;

public record EntradaLoteRequest(String tipoSanguineo, Integer quantidadeUnidades,
                                 LocalDate dataColeta, LocalDate dataValidade) {
}