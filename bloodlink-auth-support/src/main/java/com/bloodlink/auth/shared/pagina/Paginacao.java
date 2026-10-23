package com.bloodlink.auth.shared.pagina;

public final class Paginacao {

    public static final int TAMANHO_MAXIMO = 100;

    private Paginacao() {}

    public static void validar (int pagina, int tamanho) {
        if (pagina < 0) {
            throw new IllegalArgumentException("A página deve ser maior ou igual a 0");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("O tamanho deve estar entre 1 e " + TAMANHO_MAXIMO);
        }
    }
}
