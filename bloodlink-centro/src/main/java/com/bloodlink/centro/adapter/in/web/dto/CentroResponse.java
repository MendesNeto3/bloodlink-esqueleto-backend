package com.bloodlink.centro.adapter.in.web.dto;

import com.bloodlink.centro.domain.Centro;

import java.util.UUID;

public record CentroResponse(
        UUID id,
        String nome
) {
    public static CentroResponse toResponse(Centro centro) {
        return new CentroResponse(
                centro.id(),
                centro.nome()
        );
    }
}
