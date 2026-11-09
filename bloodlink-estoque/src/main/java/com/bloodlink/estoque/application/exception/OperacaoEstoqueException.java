package com.bloodlink.estoque.application.exception;

public class OperacaoEstoqueException extends RuntimeException {

    /** "operacao" é uma frase curta com o verbo, por exemplo "dar baixa no estoque". */
    public OperacaoEstoqueException(String operacao) {
        super("Não foi possível " + operacao + " no momento");
    }
}