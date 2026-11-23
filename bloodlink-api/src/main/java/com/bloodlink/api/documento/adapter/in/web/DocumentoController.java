package com.bloodlink.api.documento.adapter.in.web;

import com.bloodlink.api.documento.adapter.in.web.dto.DocumentoResponse;
import com.bloodlink.api.documento.application.port.in.DocumentoUseCase;
import com.bloodlink.api.documento.application.port.out.EnviarDocumentoComando;
import com.bloodlink.api.documento.application.port.out.FiltroDocumento;
import com.bloodlink.api.documento.domain.CategoriaDocumento;
import com.bloodlink.api.documento.exception.OperacaoDocumentoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Documentos vistos pela equipe do hemocentro. O doador usa /api/documentos/me. */
@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoUseCase documentoUseCase;

    public DocumentoController(DocumentoUseCase documentoUseCase) {
        this.documentoUseCase = documentoUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> enviar(
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "doadorId", required = false) UUID doadorId,
            Authentication autenticacao) {
        var criado = documentoUseCase.enviar(new EnviarDocumentoComando(doadorId, categoria,
                nomeDe(arquivo), bytesDe(arquivo), UUID.fromString(autenticacao.getName()),
                papelDe(autenticacao), false));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentoResponse.de(criado));
    }

    @GetMapping
    public ResponseEntity<List<DocumentoResponse>> listar(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) UUID doadorId,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "100") int limite) {
        CategoriaDocumento filtroCategoria = StringUtils.hasText(categoria) ? CategoriaDocumento.de(categoria) : null;
        return ResponseEntity.ok(documentoUseCase
                .listar(new FiltroDocumento(filtroCategoria, doadorId, busca), limite)
                .stream().map(DocumentoResponse::de).toList());
    }

    @GetMapping("/{id}/arquivo")
    public ResponseEntity<byte[]> baixar(@PathVariable UUID id) {
        return EntregaArquivo.de(documentoUseCase.baixar(id, null));
    }

    static String nomeDe(MultipartFile arquivo) {
        return arquivo == null ? null : arquivo.getOriginalFilename();
    }

    static byte[] bytesDe(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Selecione um arquivo para enviar");
        }
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new OperacaoDocumentoException("ler o arquivo enviado", e);
        }
    }

    /** ROLE_ENFERMEIRO vira ENFERMEIRO. */
    static String papelDe(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .findFirst().map(p -> p.startsWith("ROLE_") ? p.substring(5) : p).orElse("DESCONHECIDO");
    }
}
