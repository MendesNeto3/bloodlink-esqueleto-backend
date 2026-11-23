package com.bloodlink.api.reserva.exception;

import com.bloodlink.api.shared.tenant.exception.FalhaInternaException;

public class OperacaoReservaException extends FalhaInternaException {

    /** "operacao" é uma frase curta com o verbo, por exemplo "registrar a reserva". */
    public OperacaoReservaException(String operacao, Throwable causa) {
        super("Não foi possível " + operacao + " no momento", causa);
    }
}
