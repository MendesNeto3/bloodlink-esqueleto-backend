package com.bloodlink.api.documento.domain;

import java.util.Locale;

public enum CategoriaDocumento {
    EXAME(true), TERMO(true), TRIAGEM(false), CERTIFICADO(false);

    private final boolean doadorPodeEnviar;

    CategoriaDocumento(boolean doadorPodeEnviar) {
        this.doadorPodeEnviar = doadorPodeEnviar;
    }

    /** Triagem e certificado são emitidos pelo hemocentro: o doador só envia exame e termo. */
    public boolean doadorPodeEnviar() {
        return doadorPodeEnviar;
    }

    public static CategoriaDocumento de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("A categoria é obrigatória");
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Categoria inválida: use EXAME, TERMO, TRIAGEM ou CERTIFICADO");
        }
    }
}
