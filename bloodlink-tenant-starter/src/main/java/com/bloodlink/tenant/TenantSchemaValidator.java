package com.bloodlink.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public final class TenantSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(TenantSchemaValidator.class);

    private static final Pattern SCHEMA_VALIDO = Pattern.compile("^(public|centro_[a-f0-9]{12})$");
    private static final Pattern SCHEMA_TENANT_VALIDO = Pattern.compile("^centro_[a-f0-9]{12}$");

    public static void validarFormato (String schema) {
        if (schema == null || !SCHEMA_VALIDO.matcher(schema).matches()) {
            log.warn("Schema em formato inválido rejeitado: {}", schema);
            throw new IllegalArgumentException("Schema inválido: " + schema);
        }
    }

    public static void validarFormatoTenant (String schema, String tenant) {
        if (schema == null || !SCHEMA_TENANT_VALIDO.matcher(schema).matches()) {
            log.warn("Schema de tenant em formato inválido rejeitado: {}", schema);
            throw new IllegalArgumentException("Schema de tenant inválido: " + schema);
        }
    }

    public static String validarFormatoProvisionamento(String schema) {
        if (schema == null || !SCHEMA_TENANT_VALIDO.matcher(schema).matches()) {
            log.warn("Schema de provisionamento em formato inválido rejeitado: {}", schema);
            throw new IllegalArgumentException("Schema de provisionamento inválido: " + schema);
        }
        return schema;
    }

    private TenantSchemaValidator() {
    }
}
