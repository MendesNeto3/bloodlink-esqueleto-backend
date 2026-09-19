package com.bloodlink.centro.usuario.domain;

import java.util.UUID;

public record Usuario (
        UUID id,
        String email,
        String senhaHash,
        com.bloodlink.common.correlation.Papel papel,
        UUID centroId
) {
}
