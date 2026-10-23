package com.bloodlink.auth.usuario.application.in.web;

import com.bloodlink.auth.shared.exceptions.AcessoNegadoException;
import com.bloodlink.auth.usuario.application.in.AtivarContaUseCase;
import com.bloodlink.auth.usuario.application.in.CriarContaDoadorUseCase;
import com.bloodlink.auth.usuario.application.out.AutenticacaoUseCase;
import com.bloodlink.auth.usuario.application.out.RenovarTokenUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
public class AuthController {

    private static final String HEADER_CHAVE = "X-Servico-Chave";

    private final AutenticacaoUseCase autenticacao;
    private final RenovarTokenUseCase renovar;
    private final AtivarContaUseCase ativarConta;
    private final CriarContaDoadorUseCase criarContaDoador;
    private final byte[] chaveServico;

    public AuthController(AutenticacaoUseCase autenticacao,
                          RenovarTokenUseCase renovar,
                          AtivarContaUseCase ativarConta,
                          CriarContaDoadorUseCase criarContaDoador,
                          @Value("${bloodlink.servico.chave:}") String chaveServico) {
        this.autenticacao = autenticacao;
        this.renovar = renovar;
        this.ativarConta = ativarConta;
        this.criarContaDoador = criarContaDoador;
        this.chaveServico = chaveServico.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        exigirCorpo(request);
        return TokenResponse.de(autenticacao.login(request.paraComando()));
    }

    @PostMapping("/auth/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest request) {
        exigirCorpo(request);
        return TokenResponse.de(renovar.renovar(request.refreshToken()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        exigirCorpo(request);
        autenticacao.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** Pública: o doador define a senha com o token recebido por e-mail. */
    @PostMapping("/auth/ativar")
    public ResponseEntity<Void> ativar(@RequestBody AtivarContaRequest request) {
        exigirCorpo(request);
        ativarConta.ativar(request.paraComando());
        return ResponseEntity.noContent().build();
    }

    /**
     * Rota interna (a API chama ao cadastrar o doador). Não é roteada pelo gateway
     * e exige a chave de serviço.
     */
    @PostMapping("/internal/contas-doador")
    public ResponseEntity<ContaDoadorResponse> criarContaDoador(
            @RequestHeader(value = HEADER_CHAVE, required = false) String chaveInformada,
            @RequestBody CriarContaDoadorRequest request) {
        exigirChaveDeServico(chaveInformada);
        exigirCorpo(request);
        UUID usuarioId = criarContaDoador.criar(request.paraComando());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ContaDoadorResponse(usuarioId));
    }

    /** Sem chave configurada, a rota fica fechada para todos. Comparação em tempo constante. */
    private void exigirChaveDeServico(String informada) {
        if (chaveServico.length == 0 || informada == null
                || !MessageDigest.isEqual(chaveServico, informada.getBytes(StandardCharsets.UTF_8))) {
            throw new AcessoNegadoException("Acesso negado");
        }
    }

    private static void exigirCorpo(Object request) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
    }
}
