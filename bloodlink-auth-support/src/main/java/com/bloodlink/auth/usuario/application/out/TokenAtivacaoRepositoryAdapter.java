package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.usuario.application.service.TokenAtivacao;
import com.bloodlink.auth.usuario.persistance.TokenAtivacaoJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class TokenAtivacaoRepositoryAdapter implements TokenAtivacaoRepositoryPort {

    private final TokenAtivacaoJpaRepository jpa;

    public TokenAtivacaoRepositoryAdapter(TokenAtivacaoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public TokenAtivacao salvar(TokenAtivacao token) {
        return jpa.saveAndFlush(TokenAtivacaoJpaEntity.de(token)).paraDominio();
    }

    @Override
    public Optional<TokenAtivacao> buscarPorHash(String hash) {
        return jpa.findByTokenHash(hash).map(TokenAtivacaoJpaEntity::paraDominio);
    }

    @Override
    public boolean marcarUsadoSeDisponivel(UUID id, Instant agora) {
        return jpa.marcarUsado(id, agora) == 1;
    }

    @Override
    public void invalidarPendentesDoUsuario(UUID usuarioId, Instant agora) {
        jpa.invalidarPendentes(usuarioId, agora);
    }
}
