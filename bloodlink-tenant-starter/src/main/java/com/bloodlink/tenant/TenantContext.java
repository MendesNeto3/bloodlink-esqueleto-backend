package com.bloodlink.tenant;

public final class TenantContext {

    public static final String SCHEMA_PUBLICO = "public";
    private static final ThreadLocal<String> ATUAL =  new ThreadLocal<>();

    public static void set (String schema) {
        ATUAL.set(schema);
    }

    public static String get () {
        String schema = ATUAL.get();
        return schema != null ? schema : SCHEMA_PUBLICO;
    }

    public static void clear () {
        ATUAL.remove();
    }

    private TenantContext() {}
}
