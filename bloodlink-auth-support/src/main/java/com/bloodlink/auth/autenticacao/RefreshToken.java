package com.bloodlink.auth.autenticacao;

import java.time.Instant;
import java.util.UUID;

/** Guarda só o hash (SHA-256) do token: o valor em texto existe apenas na resposta ao cliente. */
public record RefreshToken(UUID id, UUID usuarioId, String tokenHash, Instant expiraEm,
                           boolean revogado, Instant criadoEm) {

    public boolean expirado(Instant agora) {
        return !expiraEm.isAfter(agora);
    }

    @Override
    public String toString() {
        return "RefreshToken[id=" + id + ", usuarioId=" + usuarioId + ", revogado=" + revogado + "]";
    }
}