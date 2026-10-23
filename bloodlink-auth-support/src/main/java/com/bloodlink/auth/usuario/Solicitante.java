package com.bloodlink.auth.usuario;

import java.util.UUID;

public record Solicitante(UUID id, Papel papel, String centroId) {

    public boolean ehPlataforma() {
        return papel == Papel.ADMIN_PLATAFORMA;
    }
}
