package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.autenticacao.RefreshToken;
import com.bloodlink.auth.autenticacao.application.out.RefreshTokenRepositoryPort;
import com.bloodlink.auth.usuario.persistance.RefreshTokenJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort, RevogarSessoesPort {

    private final RefreshTokenJpaRepository jpa;

    RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RefreshToken salvar(RefreshToken token) {
        return jpa.saveAndFlush(RefreshTokenJpaEntity.de(token)).paraDominio();
    }

    @Override
    public Optional<RefreshToken> buscarPorHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenJpaEntity::paraDominio);
    }

    @Override
    public boolean revogarSeAtivo(UUID id) {
        return jpa.revogarSeAtivo(id) == 1;
    }

    @Override
    public void revogarTodosDoUsuario(UUID usuarioId) {
        jpa.revogarTodosDoUsuario(usuarioId);
    }

    @Override
    public void revogarTodasDoUsuario(UUID usuarioId) {
        revogarTodosDoUsuario(usuarioId);
    }
}