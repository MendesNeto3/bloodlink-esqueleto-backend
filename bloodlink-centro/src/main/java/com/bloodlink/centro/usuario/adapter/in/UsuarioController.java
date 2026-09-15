package com.bloodlink.centro.usuario.adapter.in;

import com.bloodlink.centro.usuario.adapter.in.dto.CadastrarUsuarioRequest;
import com.bloodlink.centro.usuario.adapter.in.dto.UsuarioResponse;
import com.bloodlink.centro.usuario.application.port.in.CadastrarUsuarioUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final CadastrarUsuarioUseCase cadastrarUsuarioUseCase;

    public UsuarioController(CadastrarUsuarioUseCase cadastrarUsuarioUseCase) {
        this.cadastrarUsuarioUseCase = cadastrarUsuarioUseCase;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody @Valid CadastrarUsuarioRequest request) {
        var usuario = cadastrarUsuarioUseCase.cadastrar(request.email(), request.senha(), request.papel(), request.centroId());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(usuario));
    }

    @PostMapping
    public ResponseEntity
}
