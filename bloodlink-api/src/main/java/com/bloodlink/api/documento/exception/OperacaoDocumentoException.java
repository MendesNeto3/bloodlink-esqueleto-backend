package com.bloodlink.api.documento.exception;

import com.bloodlink.api.shared.tenant.exception.FalhaInternaException;

public class OperacaoDocumentoException extends FalhaInternaException {

    /** "operacao" é uma frase curta com o verbo, por exemplo "enviar o documento". */
    public OperacaoDocumentoException(String operacao, Throwable causa) {
        super("Não foi possível " + operacao + " no momento", causa);
    }
}
