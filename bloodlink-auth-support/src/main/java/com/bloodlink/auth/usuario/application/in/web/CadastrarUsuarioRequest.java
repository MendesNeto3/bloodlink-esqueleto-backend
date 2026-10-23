package com.bloodlink.auth.usuario.application.in.web;

import com.bloodlink.auth.usuario.application.in.CadastrarUsuarioCommand;

public record CadastrarUsuarioRequest(String nome, String email, String senha, String papel, String centroId) {

    public CadastrarUsuarioCommand paraComando() {
        return new CadastrarUsuarioCommand(nome, email, senha, papel, centroId);
    }

    /** Sem senha no toString: este objeto pode acabar em log por engano. */
    @Override
    public String toString() {
        return "CadastrarUsuarioRequest[papel=" + papel + ", centroId=" + centroId + "]";
    }
}
