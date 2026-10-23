package com.bloodlink.auth.usuario.application.out;

public record TokensEmitidos(String accessToken, String refreshToken, long expiraEmSegundos,
                             String nome, String papel, String centroId) {

    public TokensEmitidos(String accessToken, String refreshToken, long expiraEmSegundos) {
        this(accessToken, refreshToken, expiraEmSegundos, null, null, null);
    }

    @Override
    public String toString() {
        return "TokensEmitidos[expiraEmSegundos=" + expiraEmSegundos + "]";
    }
}
