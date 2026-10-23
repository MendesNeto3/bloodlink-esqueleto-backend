package com.bloodlink.api.doador.doadorException;

import com.bloodlink.api.shared.tenant.exception.FalhaInternaException;

public class RegistroDoadorException extends FalhaInternaException {

    public RegistroDoadorException(Throwable causa) {
        super("Falha ao registrar o doador", causa);
    }
}
