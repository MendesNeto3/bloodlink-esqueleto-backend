package com.bloodlink.centro.usuario.application.service;

public class UsuarioNaoEncontradoException extends RuntimeException {
    public UsuarioNaoEncontradoException() {
        super("Usuario nao encontrado: ");
    }
}
