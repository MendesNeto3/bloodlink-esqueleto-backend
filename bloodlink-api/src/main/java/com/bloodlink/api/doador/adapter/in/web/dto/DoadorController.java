package com.bloodlink.api.doador.adapter.in.web.dto;

import com.bloodlink.api.doador.application.port.in.*;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.shared.Pagina.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/doadores")
public class DoadorController {

    private final RegistrarDoadorUseCase registrarUseCase;
    private final ConsultarDoadorUseCase consultarUseCase;
    private final ListarDoadoresUseCase listarUseCase;
    private final AtualizarDoadorUseCase atualizarUseCase;
    private final AlterarSituacaoDoadorUseCase situacaoUseCase;

    public DoadorController(RegistrarDoadorUseCase registrarUseCase,
                            ConsultarDoadorUseCase consultarUseCase,
                            ListarDoadoresUseCase listarUseCase,
                            AtualizarDoadorUseCase atualizarUseCase,
                            AlterarSituacaoDoadorUseCase situacaoUseCase) {
        this.registrarUseCase = registrarUseCase;
        this.consultarUseCase = consultarUseCase;
        this.listarUseCase = listarUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.situacaoUseCase = situacaoUseCase;
    }

    @PostMapping
    public ResponseEntity<DoadorResponse> registrar (@RequestBody RegistrarDoadorRequest request) {
        Doador doador = registrarUseCase.registrar(request.paraComando());
        return ResponseEntity.created(URI.create("/api/doadores/" + doador.id()))
                .body(DoadorResponse.de(doador));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoadorResponse> consultar(@PathVariable UUID id) {
        return ResponseEntity.ok(DoadorResponse.de(consultarUseCase.porId(id)));
    }

    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<DoadorResponse> consultarPorCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(DoadorResponse.de(consultarUseCase.porCpf(cpf)));
    }

    @GetMapping
    public ResponseEntity<Pagina<DoadorResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String tipoSanguineo,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                listarUseCase.listar(nome, tipoSanguineo, ativo, page, size).mapear(DoadorResponse::de));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoadorResponse> atualizar(@PathVariable UUID id,
                                                    @RequestBody AtualizarDoadorRequest request) {
        return ResponseEntity.ok(DoadorResponse.de(atualizarUseCase.atualizar(id, request.paraComando())));
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<DoadorResponse> inativar(@PathVariable UUID id) {
        return ResponseEntity.ok(DoadorResponse.de(situacaoUseCase.inativar(id)));
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<DoadorResponse> reativar(@PathVariable UUID id) {
        return ResponseEntity.ok(DoadorResponse.de(situacaoUseCase.reativar(id)));
    }
}
