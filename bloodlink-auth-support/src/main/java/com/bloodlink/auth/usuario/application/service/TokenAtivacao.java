package com.bloodlink.auth.usuario.application.service;

import java.time.Instant;
import java.util.UUID;

public record TokenAtivacao(UUID id, UUID usuarioId, String tokenHash, Instant expiraEm,
                            Instant usadoEm, Instant criadoEm) {

    public boolean expirado(Instant agora) {
        return !expiraEm.isAfter(agora);
    }

    @Override
    public String toString() {
        return "TokenAtivacao[id=" + id + ", usuarioId=" + usuarioId + "]";
    }
}
