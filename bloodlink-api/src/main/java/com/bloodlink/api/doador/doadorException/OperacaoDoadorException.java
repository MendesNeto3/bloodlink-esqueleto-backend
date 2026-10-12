package com.bloodlink.api.doador.doadorException;

import com.bloodlink.api.shared.tenant.exception.FalhaInternaException;

import java.util.UUID;

public class OperacaoDoadorException extends FalhaInternaException {
    public OperacaoDoadorException(String operacao, UUID doadorId, Throwable causa) {
        super("Falha ao " + operacao + " o doador", causa);
    }
}