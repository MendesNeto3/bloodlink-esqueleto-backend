package com.bloodlink.estoque.adapter.in.web.dto;

import com.bloodlink.estoque.domain.Lote;

import java.time.LocalDate;
import java.util.UUID;

public record LoteResponse(UUID id, String tipoSanguineo, String rotulo, int quantidadeUnidades,
                           LocalDate dataColeta, LocalDate dataValidade) {

    public static LoteResponse de(Lote l) {
        return new LoteResponse(l.id(), l.tipoSanguineo().name(), l.tipoSanguineo().rotulo(),
                l.quantidadeUnidades(), l.dataColeta(), l.dataValidade());
    }
}