package com.bloodlink.auth.usuario.application.in.web;


/** Usado no refresh e no logout. */
public record RefreshRequest(String refreshToken) {

    /** O refresh token é uma credencial: nunca deve aparecer em log. */
    @Override
    public String toString() {
        return "RefreshRequest[***]";
    }
}
