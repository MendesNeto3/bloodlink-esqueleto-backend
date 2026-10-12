package com.bloodlink.api.shared.Conversor;

import java.util.Arrays;
import java.util.Locale;

public  final class ConversorEnum {
    private ConversorEnum() {}

    public static <E extends Enum<E>> E obrigatorio(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório");
        }
        return converter(tipo, valor, campo);
    }

    public static <E extends Enum<E>> E opcional(Class<E> tipo, String valor, String campo) {
        return (valor == null || valor.isBlank()) ? null : converter(tipo, valor, campo);
    }

    private static <E extends Enum<E>> E converter(Class<E> tipo, String valor, String campo) {
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor inválido para '" + campo + "': " + valor
                    + ". Valores aceitos: " + Arrays.toString(tipo.getEnumConstants()));
        }
    }
}
