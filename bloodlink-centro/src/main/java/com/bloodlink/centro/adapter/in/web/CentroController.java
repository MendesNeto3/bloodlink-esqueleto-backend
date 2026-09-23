package com.bloodlink.centro.adapter.in.web;

import com.bloodlink.centro.adapter.in.web.dto.AtualizarNomeCentroRequest;
import com.bloodlink.centro.adapter.in.web.dto.CentroResponse;
import com.bloodlink.centro.adapter.in.web.dto.RegistrarCentroRequest;
import com.bloodlink.centro.application.port.in.CentroUseCase;
import com.bloodlink.centro.config.ExigePapel;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/centros")
public class CentroController {

    private final CentroUseCase centroUseCase;

    public CentroController(CentroUseCase centroUseCase) {
        this.centroUseCase = centroUseCase;
    }

    @PostMapping
    public ResponseEntity<CentroResponse> registar (@RequestBody @Valid RegistrarCentroRequest request) {
        var centro = centroUseCase.registrar(request.nome());
        return ResponseEntity.status(HttpStatus.CREATED).body(CentroResponse.toResponse(centro));
    }

    @PatchMapping("/{id}/desativar")
    @ExigePapel({"ADMIN"})
    public ResponseEntity<CentroResponse> desativar (@PathVariable UUID id) {
        var centro = centroUseCase.desativar(id);
        return ResponseEntity.ok(CentroResponse.toResponse(centro));
    }

    @PatchMapping("/{id}")
    @ExigePapel({"ADMIN", "GESTOR"})
    public ResponseEntity<CentroResponse> atualizarNome (
            @PathVariable UUID id,
            @RequestBody @Valid AtualizarNomeCentroRequest request) {
        var centro = centroUseCase.atualizarNome(id, request.nome());
        return ResponseEntity.ok(CentroResponse.toResponse(centro));
    }

    @GetMapping
    public List<CentroResponse> listar() {
        return centroUseCase
                .listar()
                .stream()
                .map(CentroResponse::toResponse)
                .toList();
    }
}
