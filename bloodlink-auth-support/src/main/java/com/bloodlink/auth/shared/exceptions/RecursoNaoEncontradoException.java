package com.bloodlink.auth.shared.exceptions;

public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " não encontrado: " + id);
    }
}
