package com.bloodlink.auth.usuario.application.in.web;

public record RedefinirSenhaCommand(String token, String senha) {
    @Override
    public String toString() {
        return "RedefinirSenhaCommand[***]";
    }
}
