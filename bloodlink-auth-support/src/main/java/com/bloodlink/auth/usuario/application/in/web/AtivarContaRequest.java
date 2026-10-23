package com.bloodlink.auth.usuario.application.in.web;

public record AtivarContaRequest(String token, String senha) {
    public AtivarContaCommand paraComando() {
        return new AtivarContaCommand(token, senha);
    }

    @Override
    public String toString() {
        return "AtivarContaRequest[***]";
    }
}