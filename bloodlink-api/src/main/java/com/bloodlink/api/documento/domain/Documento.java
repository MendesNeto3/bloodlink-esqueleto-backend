package com.bloodlink.api.documento.domain;

import java.time.Instant;
import java.util.UUID;

/** doadorId é nulo nos documentos internos do hemocentro (ex.: ficha de triagem de um lote). */
public record Documento(UUID id,
                        UUID doadorId,
                        CategoriaDocumento categoria,
                        String nomeArquivo,
                        String tipoConteudo,
                        long tamanhoBytes,
                        String chaveArmazenamento,
                        UUID enviadoPor,
                        String enviadoPorPapel,
                        Instant enviadoEm) {

    /** Nome do arquivo pode conter o nome do doador: não sai em log. */
    @Override
    public String toString() {
        return "Documento[id=" + id + "]";
    }
}
