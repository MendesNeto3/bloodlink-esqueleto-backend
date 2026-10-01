package com.bloodlink.centro.usuario.domain;

import com.bloodlink.common.correlation.Papel;

import java.util.UUID;

public record Usuario (
        UUID id,
        String email,
        String senhaHash,
        Papel papel,
        UUID centroId
) {
}
