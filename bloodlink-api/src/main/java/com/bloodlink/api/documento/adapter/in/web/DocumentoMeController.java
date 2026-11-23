package com.bloodlink.api.documento.adapter.in.web;

import com.bloodlink.api.documento.adapter.in.web.dto.DocumentoResponse;
import com.bloodlink.api.documento.application.port.in.DocumentoUseCase;
import com.bloodlink.api.documento.application.port.out.EnviarDocumentoComando;
import com.bloodlink.common.seguranca.JwtAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** Área do próprio doador: o id vem do token (cabeçalho montado pelo filtro), nunca do corpo da requisição. */
@RestController
@RequestMapping("/api/documentos/me")
public class DocumentoMeController {

    private final DocumentoUseCase documentoUseCase;

    public DocumentoMeController(DocumentoUseCase documentoUseCase) {
        this.documentoUseCase = documentoUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> enviar(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId,
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
            @RequestParam(value = "categoria", required = false) String categoria) {
        var criado = documentoUseCase.enviar(new EnviarDocumentoComando(doadorId, categoria,
                DocumentoController.nomeDe(arquivo), DocumentoController.bytesDe(arquivo),
                doadorId, "DOADOR", true));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentoResponse.de(criado));
    }

    @GetMapping
    public ResponseEntity<List<DocumentoResponse>> meusDocumentos(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId) {
        return ResponseEntity.ok(documentoUseCase.listarDoDoador(doadorId).stream()
                .map(DocumentoResponse::de).toList());
    }

    @GetMapping("/{id}/arquivo")
    public ResponseEntity<byte[]> baixar(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId,
            @PathVariable UUID id) {
        return EntregaArquivo.de(documentoUseCase.baixar(id, doadorId));
    }
}
