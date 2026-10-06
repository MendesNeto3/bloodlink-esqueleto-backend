package com.bloodlink.api.shared.tenant.exception;

public class RetomadaProvisionamentoException extends RuntimeException {
    private final String schema;

    public RetomadaProvisionamentoException(String schema, Throwable cause) {
        super("Falha ao retomar o provisionamento do schema " + schema, cause);
        this.schema = schema;
    }

    public String getSchema() {
        return schema;
    }
}
