package com.bloodlink.estoque.adapter.in.web.dto;

import com.bloodlink.estoque.application.port.in.EstoqueUseCase;

public record BaixaResponse(String tipoSanguineo, String rotulo, int unidadesRetiradas, long unidadesRestantes) {

    public static BaixaResponse de(EstoqueUseCase.ResultadoBaixa r) {
        return new BaixaResponse(r.tipoSanguineo().name(), r.tipoSanguineo().rotulo(),
                r.unidadesRetiradas(), r.unidadesRestantes());
    }
}