package com.bloodlink.checkin.domain;

import java.util.Locale;

public enum ResultadoTriagem {
    APTA, INAPTA;

    public static ResultadoTriagem de (String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O resultado da triagem é obrigatório (APTA ou INAPTA)");
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Resultado da triagem inválido: use APTA ou INAPTA");
        }
    }
}
