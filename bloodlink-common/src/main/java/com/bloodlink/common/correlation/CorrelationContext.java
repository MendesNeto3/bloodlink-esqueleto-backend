package com.bloodlink.common.correlation;

import java.util.UUID;

public class CorrelationContext {

    public static final String HEADER = "X-Correlation-Id";
    private static final ThreadLocal<String> ATUAL = new ThreadLocal<>();

    public static void set(String correlationId) {
        ATUAL.set(correlationId !=  null ? correlationId : gerar());
    }
    public static String get() {
        String id = ATUAL.get();
        return id != null ? id : gerar();
    }
    public static void clear() {
        ATUAL.remove();
    }
    public static String gerar() {
        return UUID.randomUUID().toString();
    }

    private CorrelationContext() {}
}