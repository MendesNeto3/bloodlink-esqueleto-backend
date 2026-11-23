package com.bloodlink.auth.usuario.application.in.web;

public record EsqueciSenhaRequest(String email) {
    @Override
    public String toString() {
        return "EsqueciSenhaRequest[***]";
    }
}
