package com.bloodlink.centro.adapter.in.web;

import com.bloodlink.centro.adapter.in.web.dto.*;
import com.bloodlink.centro.application.port.in.CentroUseCase;
import com.bloodlink.common.seguranca.JwtAuthenticationFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/centros")
public class CentroController {

    private static final String ROLE_PLATAFORMA = "ROLE_ADMIN_PLATAFORMA";

    private final CentroUseCase centroUseCase;

    public CentroController(CentroUseCase centroUseCase) {
        this.centroUseCase = centroUseCase;
    }

    @PostMapping
    public ResponseEntity<CentroResponse> registar(@RequestBody @Valid RegistrarCentroRequest request) {
        var centro = centroUseCase.registrar(request.nome());
        return ResponseEntity.status(HttpStatus.CREATED).body(CentroResponse.toResponse(centro));
    }

    @GetMapping
    public List<CentroResponse> listar() {
        return centroUseCase.listar()
                .stream()
                .map(CentroResponse::toResponse)
                .toList();
    }

    // pública: só centros ativos, sem schema nem id interno. Com lat e lng calcula a distância e ordena.
    @GetMapping("/publicos")
    public List<CentroPublicoResponse> listarPublicos(@RequestParam(required = false) Double lat,
                                                      @RequestParam(required = false) Double lng) {
        return centroUseCase.listarPublicos(lat, lng)
                .stream()
                .map(CentroPublicoResponse::de)
                .toList();
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<CentroResponse> desativar(@PathVariable UUID id) {
        var centro = centroUseCase.desativar(id);
        return ResponseEntity.ok(CentroResponse.toResponse(centro));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CentroResponse> atualizarNome(
            @PathVariable UUID id,
            @RequestBody @Valid AtualizarNomeCentroRequest request,
            Authentication authentication,
            @RequestHeader(value = JwtAuthenticationFilter.HEADER_TENANT, required = false) String centroIdDoToken) {
        var centro = centroUseCase.atualizarNome(id, request.nome(), solicitante(authentication, centroIdDoToken));
        return ResponseEntity.ok(CentroResponse.toResponse(centro));
    }

    @PatchMapping("/{id}/perfil")
    public ResponseEntity<CentroPublicoResponse> atualizarPerfil(
            @PathVariable UUID id,
            @RequestBody AtualizarPerfilCentroRequest request,
            Authentication authentication,
            @RequestHeader(value = JwtAuthenticationFilter.HEADER_TENANT, required = false) String centroIdDoToken) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        var centro = centroUseCase.atualizarPerfil(id, request.paraPerfil(), solicitante(authentication, centroIdDoToken));
        return ResponseEntity.ok(CentroPublicoResponse.de(centro, null));
    }

    /** null = plataforma (sem restrição). Para os demais, o centroId do token; ausente vira "" e nunca confere. */
    private static String solicitante(Authentication authentication, String centroIdDoToken) {
        boolean plataforma = authentication.getAuthorities().stream()
                .anyMatch(a -> ROLE_PLATAFORMA.equals(a.getAuthority()));
        if (plataforma) {
            return null;
        }
        return centroIdDoToken == null ? "" : centroIdDoToken;
    }
}