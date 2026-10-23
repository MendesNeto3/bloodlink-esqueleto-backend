package com.bloodlink.auth.usuario.application.in.web;

import com.bloodlink.auth.usuario.application.out.TokensEmitidos;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                            String nome, String papel, String centroId) {

    public static TokenResponse de(TokensEmitidos t) {
        return new TokenResponse(t.accessToken(), t.refreshToken(), "Bearer", t.expiraEmSegundos(),
                t.nome(), t.papel(), t.centroId());
    }

    @Override
    public String toString() {
        return "TokenResponse[tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}