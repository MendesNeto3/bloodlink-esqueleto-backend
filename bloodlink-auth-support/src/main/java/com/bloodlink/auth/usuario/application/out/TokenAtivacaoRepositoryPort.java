package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.usuario.application.service.TokenAtivacao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenAtivacaoRepositoryPort {
    TokenAtivacao salvar(TokenAtivacao token);

    Optional<TokenAtivacao> buscarPorHash(String hash);

    /** Atômico: só marca se ainda não foi usado e não expirou. Devolve false se perdeu a corrida. */
    boolean marcarUsadoSeDisponivel(UUID id, Instant agora);

    void invalidarPendentesDoUsuario(UUID usuarioId, Instant agora);
}