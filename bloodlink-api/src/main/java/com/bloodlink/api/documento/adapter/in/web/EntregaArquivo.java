package com.bloodlink.api.documento.adapter.in.web;

import com.bloodlink.api.documento.domain.ArquivoBaixado;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Monta a resposta de download: sempre como anexo, sem cache e sem deixar o navegador adivinhar o tipo. */
final class EntregaArquivo {

    private EntregaArquivo() {
    }

    static ResponseEntity<byte[]> de(ArquivoBaixado arquivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.tipoConteudo()))
                .contentLength(arquivo.bytes().length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nome(), StandardCharsets.UTF_8)
                                .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(arquivo.bytes());
    }
}
