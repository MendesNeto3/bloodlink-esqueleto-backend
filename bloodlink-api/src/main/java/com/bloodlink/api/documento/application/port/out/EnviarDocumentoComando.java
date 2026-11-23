package com.bloodlink.api.documento.application.port.out;

import java.util.UUID;

/**
 * Arquivo recebido, ainda sem validação. O tipo informado pelo navegador é ignorado:
 * o tipo real vem dos primeiros bytes do arquivo.
 */
public record EnviarDocumentoComando(UUID doadorId,
                                     String categoria,
                                     String nomeOriginal,
                                     byte[] conteudo,
                                     UUID enviadoPor,
                                     String papel,
                                     boolean enviadoPeloDoador) {

    @Override
    public String toString() {
        return "EnviarDocumentoComando[enviadoPor=" + enviadoPor + "]";
    }
}
