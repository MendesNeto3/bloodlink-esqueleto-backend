package com.bloodlink.api.shared.tenant.exception;

public class ProvisionamentoNaoEncontradoException extends RuntimeException {
    public ProvisionamentoNaoEncontradoException(String schema) {
        super("Provisionamento não encontrado para o schema: " + schema);
    }
}
