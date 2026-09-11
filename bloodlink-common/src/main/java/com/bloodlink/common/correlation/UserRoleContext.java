package com.bloodlink.common.correlation;

public final class UserRoleContext {

    public static final String HEADER = "X-User-Role";
    private static final ThreadLocal<String> ATUAL = new ThreadLocal<>();

    public static void set(String papel) {
        ATUAL.set(papel);
    }

    public static String get() {
        return ATUAL.get();
    }

    public static void clear() {
        ATUAL.remove();
    }

    private UserRoleContext() {}
}