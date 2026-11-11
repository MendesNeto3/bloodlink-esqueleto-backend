package com.bloodlink.checkin.application.exception;

public class OperacaoCheckinException extends RuntimeException {

    /** "operacao" é uma frase curta com o verbo, por exemplo "registrar o check-in". */
    public OperacaoCheckinException(String operacao) {
        super("Não foi possível " + operacao + " no momento");
    }
}