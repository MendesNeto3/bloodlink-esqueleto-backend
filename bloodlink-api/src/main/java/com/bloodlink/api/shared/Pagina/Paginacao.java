package com.bloodlink.api.shared.Pagina;

public final class Paginacao {
    public static final int TAMANHO_MAXIMO = 100;
    private Paginacao() {}

    public static void validar (int pagina, int tamanho) {
        if (pagina < 0 ) {
            throw new IllegalArgumentException("Página inválida: " + pagina + ". Use um valor maior ou igual a 0");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException(
                    "Tamanho inválido: " + tamanho + ". Use um valor entre 1 e " + TAMANHO_MAXIMO);
        }
    }
}
