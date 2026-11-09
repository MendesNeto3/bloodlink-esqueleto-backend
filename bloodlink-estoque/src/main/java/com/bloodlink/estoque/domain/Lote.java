package com.bloodlink.estoque.domain;

import java.time.LocalDate;
import java.util.UUID;

public record Lote(UUID id, TipoSanguineo tipoSanguineo, int quantidadeUnidades,
                   LocalDate dataColeta, LocalDate dataValidade, UUID registradoPor) {
}