package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.autenticacao.RefreshToken;
import com.bloodlink.auth.autenticacao.application.out.RefreshTokenRepositoryPort;
import com.bloodlink.auth.shared.exceptions.CredenciaisInvalidasException;
import com.bloodlink.auth.shared.exceptions.OperacaoAutenticacaoException;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.out.RenovarTokenUseCase;
import com.bloodlink.auth.usuario.application.out.TokensEmitidos;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class RenovarTokenService implements RenovarTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RenovarTokenService.class);

    private static final String MENSAGEM_INVALIDA = "Refresh token inválido ou expirado";

    private final RefreshTokenRepositoryPort refreshRepository;
    private final UsuarioRepositoryPort usuarioRepository;
    private final EmissaoDeTokens emissao;
    private final Clock clock;

    public RenovarTokenService(RefreshTokenRepositoryPort refreshRepository,
                               UsuarioRepositoryPort usuarioRepository,
                               EmissaoDeTokens emissao,
                               Clock clock) {
        this.refreshRepository = refreshRepository;
        this.usuarioRepository = usuarioRepository;
        this.emissao = emissao;
        this.clock = clock;
    }

    /**
     * noRollbackFor é essencial: nos ramos de reuso/corrida a revogação em massa precisa
     * ser gravada MESMO lançando a exceção 401; sem isso o rollback desfaria a revogação.
     */
    @Override
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokensEmitidos renovar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("O refresh token é obrigatório");
        }

        RefreshToken atual = buscarToken(TokensOpacos.hash(refreshToken.trim()))
                .orElseThrow(() -> new CredenciaisInvalidasException(MENSAGEM_INVALIDA));

        if (atual.revogado()) {
            // Um token já usado voltou: pode ter sido roubado. Derruba todas as sessões do usuário.
            log.warn("Reuso de refresh token detectado; revogando todas as sessões do usuário {}", atual.usuarioId());
            revogarTodos(atual);
            throw new CredenciaisInvalidasException(MENSAGEM_INVALIDA);
        }
        if (atual.expirado(Instant.now(clock))) {
            throw new CredenciaisInvalidasException(MENSAGEM_INVALIDA);
        }

        // conta inativada ou ainda pendente de ativação não renova a sessão
        Usuario usuario = buscarUsuario(atual)
                .filter(Usuario::podeLogar)
                .orElse(null);
        if (usuario == null) {
            revogarTodos(atual);
            throw new CredenciaisInvalidasException(MENSAGEM_INVALIDA);
        }

        if (!revogarSeAtivo(atual)) {
            // Outra requisição consumiu este mesmo token no mesmo instante: trata como reuso.
            log.warn("Renovação concorrente com o mesmo refresh token; revogando sessões do usuário {}", usuario.id());
            revogarTodos(atual);
            throw new CredenciaisInvalidasException(MENSAGEM_INVALIDA);
        }

        TokensEmitidos novos = emissao.emitir(usuario, "renovar o token");
        log.info("Token renovado para o usuário {}", usuario.id());
        return novos;
    }

    private Optional<RefreshToken> buscarToken(String hash) {
        try {
            return refreshRepository.buscarPorHash(hash);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o refresh token: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha ao consultar o refresh token", e);
            throw new OperacaoAutenticacaoException("Falha ao renovar o token", e);
        }
    }

    private Optional<Usuario> buscarUsuario(RefreshToken token) {
        try {
            return usuarioRepository.buscarPorId(token.usuarioId());
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o usuário na renovação: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha ao consultar o usuário na renovação", e);
            throw new OperacaoAutenticacaoException("Falha ao renovar o token", e);
        }
    }

    private boolean revogarSeAtivo(RefreshToken token) {
        try {
            return refreshRepository.revogarSeAtivo(token.id());
        } catch (RuntimeException e) {
            log.error("Falha ao revogar o refresh token {}: {}", token.id(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao revogar o refresh token", e);
            throw new OperacaoAutenticacaoException("Falha ao renovar o token", e);
        }
    }

    private void revogarTodos(RefreshToken token) {
        try {
            refreshRepository.revogarTodosDoUsuario(token.usuarioId());
        } catch (RuntimeException e) {
            log.error("Falha ao revogar as sessões do usuário {}: {}", token.usuarioId(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao revogar as sessões do usuário", e);
            throw new OperacaoAutenticacaoException("Falha ao renovar o token", e);
        }
    }
}
