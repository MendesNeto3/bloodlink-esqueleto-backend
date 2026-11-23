package com.bloodlink.auth.usuario.application.in.web;

public record RedefinirSenhaRequest(String token, String senha) {
    public RedefinirSenhaCommand paraComando() {
        return new RedefinirSenhaCommand(token, senha);
    }

    @Override
    public String toString() {
        return "RedefinirSenhaRequest[***]";
    }
}
