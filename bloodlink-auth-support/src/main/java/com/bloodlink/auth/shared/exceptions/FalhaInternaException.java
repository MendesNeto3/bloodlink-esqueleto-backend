package com.bloodlink.auth.shared.exceptions;

public class FalhaInternaException extends RuntimeException {
    protected FalhaInternaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
