package com.bloodlink.auth.usuario.application.in.web;

import com.bloodlink.auth.shared.pagina.Pagina;
import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.application.in.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final CadastrarUsuarioUseCase cadastrarUsuarioUseCase;
    private final ConsultarUsuarioUseCase consultar;
    private final ListarUsuariosUseCase listar;
    private final AlterarSenhaUseCase alterarSenha;
    private final AlterarSituacaoUsuarioUseCase alterarSituacao;

    public UsuarioController(CadastrarUsuarioUseCase cadastrarUsuarioUseCase,
                             ConsultarUsuarioUseCase consultar,
                             ListarUsuariosUseCase listar,
                             AlterarSenhaUseCase alterarSenha,
                             AlterarSituacaoUsuarioUseCase alterarSituacao) {
        this.cadastrarUsuarioUseCase = cadastrarUsuarioUseCase;
        this.consultar = consultar;
        this.listar = listar;
        this.alterarSenha = alterarSenha;
        this.alterarSituacao = alterarSituacao;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar (@RequestBody CadastrarUsuarioRequest cadastrar,
                                                      @AuthenticationPrincipal Solicitante solicitante){
        if (cadastrar == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        var criado = cadastrarUsuarioUseCase.cadastrar(cadastrar.paraComando(), solicitante);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id()))
                .body(UsuarioResponse.de(criado));
    }

    @GetMapping("/{id}")
    public UsuarioResponse consultar(@PathVariable UUID id, @AuthenticationPrincipal Solicitante solicitante) {
        return UsuarioResponse.de(consultar.porId(id, solicitante));
    }

    @GetMapping
    public Pagina<UsuarioResponse> listar(@RequestParam(required = false) String email,
                                          @RequestParam(required = false) String papel,
                                          @RequestParam(required = false) String centroId,
                                          @RequestParam(required = false) Boolean ativo,
                                          @RequestParam(name = "page", defaultValue = "0") int page,
                                          @RequestParam(name = "size", defaultValue = "20") int size,
                                          @AuthenticationPrincipal Solicitante solicitante) {
        return listar.listar(email, papel, centroId, ativo, page, size, solicitante)
                .mapear(UsuarioResponse::de);
    }

    @PatchMapping("/{id}/inativar")
    public UsuarioResponse inativar(@PathVariable UUID id, @AuthenticationPrincipal Solicitante solicitante) {
        return UsuarioResponse.de(alterarSituacao.inativar(id, solicitante));
    }

    @PatchMapping("/{id}/reativar")
    public UsuarioResponse reativar(@PathVariable UUID id, @AuthenticationPrincipal Solicitante solicitante) {
        return UsuarioResponse.de(alterarSituacao.reativar(id, solicitante));
    }

    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarSenha(@RequestBody AlterarSenhaRequest request,
                                             @AuthenticationPrincipal Solicitante solicitante) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        alterarSenha.alterar(request.paraComando(), solicitante);
        return ResponseEntity.noContent().build();
    }
}
