package com.bloodlink.common.seguranca;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

final class TenantIds {

    private static final Pattern FORMATO = Pattern.compile("^[a-f0-9]{12}$");

    private TenantIds() { }

    /**
     * Devolve o id em minúsculas se tiver o formato válido; vazio se for nulo, em branco
     * ou fora do formato. É o que fica depois de "centro_" no nome do schema.
     */
    public static Optional<String> normalizar(String centroId) {
        if (centroId == null) {
            return Optional.empty();
        }
        String limpo = centroId.trim().toLowerCase(Locale.ROOT);
        return FORMATO.matcher(limpo).matches() ? Optional.of(limpo) : Optional.empty();
    }

    public static boolean ehValido(String centroId) {
        return normalizar(centroId).isPresent();
    }
}

