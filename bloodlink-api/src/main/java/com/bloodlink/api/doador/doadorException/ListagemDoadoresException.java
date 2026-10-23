package com.bloodlink.api.doador.doadorException;

import com.bloodlink.api.shared.tenant.exception.FalhaInternaException;

public class ListagemDoadoresException extends FalhaInternaException {
    public ListagemDoadoresException(Throwable causa) {
        super("Falha ao listar os doadores", causa);
    }}
