package com.bloodlink.api.doador.doadorException;

public class ListagemDoadoresException extends RuntimeException {
    public ListagemDoadoresException(Throwable causa) {
        super("Falha ao listar os doadores", causa);
    }
}
