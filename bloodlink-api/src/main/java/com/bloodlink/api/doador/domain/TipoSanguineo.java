package com.bloodlink.api.doador.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum TipoSanguineo{
    A_POSITIVO("A+"), A_NEGATIVO("A-"),
    B_POSITIVO("B+"), B_NEGATIVO("B-"),
    AB_POSITIVO("AB+"), AB_NEGATIVO("AB-"),
    O_POSITIVO("O+"), O_NEGATIVO("O-");

    private final String rotulo;

    TipoSanguineo(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Texto exibido na tela e usado no JSON (ex.: "O-"). */
    public String rotulo() {
        return rotulo;
    }

    /** Aceita "O-", "o-" ou o nome do enum ("O_NEGATIVO"); vazio se não reconhecer. */
    public static Optional<TipoSanguineo> de(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String limpo = texto.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.rotulo.equals(limpo) || t.name().equals(limpo))
                .findFirst();
    }
}
