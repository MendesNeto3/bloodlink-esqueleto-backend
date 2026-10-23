package com.bloodlink.auth.usuario.application.in.web;

import com.bloodlink.auth.usuario.persistance.AreaAcesso;

public record LoginCommand(String email, String senha, AreaAcesso area) {

    /** Compatível com quem não informa a aba. */
    public LoginCommand(String email, String senha) {
        this(email, senha, null);
    }

    @Override
    public String toString() {
        return "LoginCommand[***]";
    }
}
