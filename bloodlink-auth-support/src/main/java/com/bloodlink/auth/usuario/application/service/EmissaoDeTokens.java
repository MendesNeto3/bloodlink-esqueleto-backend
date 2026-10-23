package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.autenticacao.RefreshToken;
import com.bloodlink.auth.autenticacao.application.out.RefreshTokenRepositoryPort;
import com.bloodlink.auth.shared.exceptions.OperacaoAutenticacaoException;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.out.EmissorTokenPort;
import com.bloodlink.auth.usuario.application.out.TokensEmitidos;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class EmissaoDeTokens {

    private static final Logger log = LoggerFactory.getLogger(EmissaoDeTokens.class);

    private final EmissorTokenPort emissor;
    private final RefreshTokenRepositoryPort refreshRepository;
    private final Clock clock;
    private final Duration validadeRefresh;

    EmissaoDeTokens(EmissorTokenPort emissor, RefreshTokenRepositoryPort refreshRepository, Clock clock,
                    @Value("${bloodlink.jwt.validade-refresh:7d}") Duration validadeRefresh) {
        this.emissor = emissor;
        this.refreshRepository = refreshRepository;
        this.clock = clock;
        this.validadeRefresh = validadeRefresh;
    }

    TokensEmitidos emitir(Usuario usuario, String operacao) {
        // última barreira: conta pendente (doador sem ativação) ou inativada nunca recebe token
        if (!usuario.podeLogar()) {
            throw new IllegalStateException("Usuário " + usuario.id() + " não pode receber tokens");
        }
        try {
            String access = emissor.emitirAccessToken(usuario);
            String refreshEmTexto = TokensOpacos.gerar();
            Instant agora = Instant.now(clock);
            refreshRepository.salvar(new RefreshToken(UUID.randomUUID(), usuario.id(),
                    TokensOpacos.hash(refreshEmTexto), agora.plus(validadeRefresh), false, agora));
            return new TokensEmitidos(access, refreshEmTexto, emissor.validadeAccessSegundos(),
                    usuario.nome(), usuario.papel().name(), usuario.centroId());        } catch (RuntimeException e) {
            log.error("Falha ao emitir tokens ({}) para o usuário {}: {}", operacao, usuario.id(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao emitir tokens", e);
            throw new OperacaoAutenticacaoException("Falha ao " + operacao, e);
        }
    }
}

