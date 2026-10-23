package com.bloodlink.auth.shared.exceptions;

public class OperacaoUsuarioException extends FalhaInternaException {

    public OperacaoUsuarioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
