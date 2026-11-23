package com.bloodlink.estoque.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum MotivoBaixa {
    TRANSFUSAO, DESCARTE, OUTRO;

    public static Optional<MotivoBaixa> de(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String limpo = texto.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(m -> m.name().equals(limpo)).findFirst();
    }
}
