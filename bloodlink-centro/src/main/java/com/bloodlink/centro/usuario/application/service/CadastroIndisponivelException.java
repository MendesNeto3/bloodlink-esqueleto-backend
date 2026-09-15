package com.bloodlink.centro.usuario.application.service;

public class CadastroIndisponivelException extends RuntimeException {
    public CadastroIndisponivelException() {
        super("Não foi possível cadastrar o usuário no momento");
    }
}

