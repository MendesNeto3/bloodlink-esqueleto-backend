package com.bloodlink.auth.shared.ConversorEnum;

import java.util.Arrays;
import java.util.Locale;

public final class ConversorEnum {
    private ConversorEnum() {}

    public static <E extends Enum<E>> E obrigatorio(Class<E> tipo, String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório");
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor inválido para '" + campo + "'. Valores aceitos: "
                    + Arrays.toString(tipo.getEnumConstants()));
        }
    }

    public static <E extends Enum<E>> E opcional(Class<E> tipo, String campo, String valor) {
        return (valor == null || valor.isBlank()) ? null : obrigatorio(tipo, campo, valor);
    }}
