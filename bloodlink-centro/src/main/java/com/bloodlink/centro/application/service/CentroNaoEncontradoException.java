package com.bloodlink.centro.application.service;

public class CentroNaoEncontradoException extends RuntimeException {
    public CentroNaoEncontradoException() {
        super("Centro não encontrado!");
    }
}
