package com.bloodlink.api.doador.doadorException;

import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;

import java.util.UUID;

public class DoadorNaoEncontradoException extends RecursoNaoEncontradoException {
    public DoadorNaoEncontradoException(UUID id) {
        super("Doador", id);
    }
}
