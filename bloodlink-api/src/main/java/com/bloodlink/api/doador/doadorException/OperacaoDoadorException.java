package com.bloodlink.api.doador.doadorException;

import com.bloodlink.api.shared.tenant.exception.FalhaInternaException;

public class OperacaoDoadorException extends FalhaInternaException {

    /** "operacao" é uma frase curta com o verbo, por exemplo "listar os doadores" ou "registrar a doação". */
    public OperacaoDoadorException(String operacao) {
        super("Não foi possível " + operacao + " no momento");
    }

    public OperacaoDoadorException(String operacao, Throwable causa) {
        super("Não foi possível " + operacao + " no momento", causa);
    }
}