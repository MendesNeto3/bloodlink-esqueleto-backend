package com.bloodlink.auth.usuario.application.in.web;

import com.bloodlink.auth.usuario.application.in.AlterarSenhaCommand;

public record AlterarSenhaRequest(String senhaAtual, String novaSenha) {

    public AlterarSenhaCommand paraComando() {
        return new AlterarSenhaCommand(senhaAtual, novaSenha);
    }

    @Override
    public String toString() {
        return "AlterarSenhaRequest[***]";
    }
}
