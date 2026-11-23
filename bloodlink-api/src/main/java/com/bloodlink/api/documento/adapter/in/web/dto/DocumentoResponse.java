package com.bloodlink.api.documento.adapter.in.web.dto;

import com.bloodlink.api.documento.domain.Documento;
import com.bloodlink.api.documento.domain.DocumentoComDoador;

import java.time.Instant;
import java.util.UUID;

/** A chave de armazenamento nunca sai pela API: o arquivo só é entregue por /arquivo, com checagem de acesso. */
public record DocumentoResponse(UUID id, String categoria, String nomeArquivo, String tipoConteudo,
                                long tamanhoBytes, UUID doadorId, String doadorNome,
                                String enviadoPorPapel, Instant enviadoEm) {

    public static DocumentoResponse de(DocumentoComDoador item) {
        Documento d = item.documento();
        return new DocumentoResponse(d.id(), d.categoria().name(), d.nomeArquivo(), d.tipoConteudo(),
                d.tamanhoBytes(), d.doadorId(), item.doadorNome(), d.enviadoPorPapel(), d.enviadoEm());
    }

    public static DocumentoResponse de(Documento d) {
        return de(new DocumentoComDoador(d, null));
    }
}
