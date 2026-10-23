package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.autenticacao.application.out.RefreshTokenRepositoryPort;
import com.bloodlink.auth.shared.exceptions.CredenciaisInvalidasException;
import com.bloodlink.auth.shared.exceptions.OperacaoAutenticacaoException;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.web.LoginCommand;
import com.bloodlink.auth.usuario.application.out.AutenticacaoUseCase;

import com.bloodlink.auth.usuario.application.out.TokensEmitidos;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class AutenticacaoService implements AutenticacaoUseCase {

    private static final Logger log = LoggerFactory.getLogger(AutenticacaoService.class);

    private static final String MENSAGEM_INVALIDA = "E-mail ou senha inválidos";
    private static final int MAX_BYTES_SENHA = 72;

    private final UsuarioRepositoryPort usuarioRepository;
    private final RefreshTokenRepositoryPort refreshRepository;
    private final PasswordEncoder encoder;
    private final EmissaoDeTokens emissao;
    /** Hash descartável para gastar o mesmo tempo quando o e-mail não existe (evita enumerar usuários). */
    private final String hashFalso;

    public AutenticacaoService(UsuarioRepositoryPort usuarioRepository, RefreshTokenRepositoryPort refreshRepository, PasswordEncoder encoder, EmissaoDeTokens emissao) {
        this.usuarioRepository = usuarioRepository;
        this.refreshRepository = refreshRepository;
        this.encoder = encoder;
        this.emissao = emissao;
        this.hashFalso = encoder.encode("senha-descartavel-para-tempo-constante");
    }

    @Override
    public TokensEmitidos login(LoginCommand comando) {
        if (comando == null || comando.email() == null || comando.email().isBlank()
                || comando.senha() == null || comando.senha().isEmpty()) {
            throw new IllegalArgumentException("E-mail e senha são obrigatórios");
        }

        String email = comando.email().trim().toLowerCase(Locale.ROOT);

        Optional<Usuario> encontrado = buscar(email);

        String hashParaComparar = encontrado
                .map(Usuario::senhaHash)
                .filter(h -> h != null && !h.isBlank())
                .orElse(hashFalso);
        boolean senhaConfere = comando.senha().getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES_SENHA
                && encoder.matches(comando.senha(), hashParaComparar);

        boolean areaConfere = comando.area() == null
                || encontrado.map(u -> comando.area().permite(u.papel())).orElse(false);

        if (encontrado.isEmpty() || !senhaConfere || !encontrado.get().podeLogar() || !areaConfere) {
            log.warn("Tentativa de login rejeitada");
            throw new CredenciaisInvalidasException(MENSAGEM_INVALIDA);
        }

        Usuario usuario = encontrado.get();
        TokensEmitidos tokens = emissao.emitir(usuario, "realizar o login");
        log.info("Login do usuário {} (papel={})", usuario.id(), usuario.papel());
        return tokens;
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("O refresh token é obrigatório");
        }
        String hash = TokensOpacos.hash(refreshToken.trim());
        try {
            refreshRepository.buscarPorHash(hash).ifPresent(t -> {
                if (refreshRepository.revogarSeAtivo(t.id())) {
                    log.info("Logout do usuário {}", t.usuarioId());
                }
            });
        } catch (RuntimeException e) {
            log.error("Falha ao realizar o logout: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha no logout", e);
            throw new OperacaoAutenticacaoException("Falha ao realizar o logout", e);
        }
    }

    private Optional<Usuario> buscar(String email) {
        try {
            return usuarioRepository.buscarPorEmail(email);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o usuário no login: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha no login", e);
            throw new OperacaoAutenticacaoException("Falha ao realizar o login", e);
        }
    }
}