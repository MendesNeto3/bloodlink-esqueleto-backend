package com.bloodlink.auth.shared.exceptions;

public class OperacaoAutenticacaoException extends FalhaInternaException  {

    public OperacaoAutenticacaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
