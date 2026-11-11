package com.bloodlink.centro.application.service;

public class ChaveDeServicoInvalidaException extends RuntimeException {

    public ChaveDeServicoInvalidaException() {
        super("Acesso negado");
    }
}