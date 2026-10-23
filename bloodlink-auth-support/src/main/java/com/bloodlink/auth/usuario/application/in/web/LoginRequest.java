package com.bloodlink.auth.usuario.application.in.web;

import com.bloodlink.auth.usuario.persistance.AreaAcesso;

public record LoginRequest(String email, String senha, AreaAcesso area) {

    public LoginCommand paraComando() {
        return new LoginCommand(email, senha, area);
    }

    @Override
    public String toString() {
        return "LoginRequest[***]";
    }
}