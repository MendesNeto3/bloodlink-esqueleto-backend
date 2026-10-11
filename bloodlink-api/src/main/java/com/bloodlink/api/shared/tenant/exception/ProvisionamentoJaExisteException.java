package com.bloodlink.api.shared.tenant.exception;

public class ProvisionamentoJaExisteException extends RuntimeException {
    public ProvisionamentoJaExisteException(String schema, Throwable cause) {
        super("Já existe provisionamento para o schema: " + schema, cause);
    }
}
