package com.bloodlink.api.shared.tenant.exception;

public abstract class FalhaInternaException extends RuntimeException {

    protected FalhaInternaException(String mensagem) {
        super(mensagem);
    }

    protected FalhaInternaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
