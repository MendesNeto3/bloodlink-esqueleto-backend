package com.bloodlink.auth.autenticacao.application.out;

import com.bloodlink.auth.autenticacao.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    RefreshToken salvar(RefreshToken token);

    Optional<RefreshToken> buscarPorHash(String tokenHash);

    /** Revoga só se ainda estiver ativo; false = outra requisição já o consumiu (protege contra corrida). */
    boolean revogarSeAtivo(UUID id);

    void revogarTodosDoUsuario(UUID usuarioId);
}
