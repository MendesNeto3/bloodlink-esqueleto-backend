package com.bloodlink.auth.usuario.persistance;

import com.bloodlink.auth.usuario.application.service.TokenAtivacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_ativacao")
public class TokenAtivacaoJpaEntity {

    @Id
    UUID id;

    @Column(name = "usuario_id", nullable = false)
    UUID usuarioId;

    @Column(name = "token_hash", nullable = false, length = 64)
    String tokenHash;

    @Column(name = "expira_em", nullable = false)
    Instant expiraEm;

    @Column(name = "usado_em")
    Instant usadoEm;

    @Column(name = "criado_em", nullable = false)
    Instant criadoEm;

    protected TokenAtivacaoJpaEntity() { }

    public static TokenAtivacaoJpaEntity de(TokenAtivacao t) {
        TokenAtivacaoJpaEntity e = new TokenAtivacaoJpaEntity();
        e.id = t.id();
        e.usuarioId = t.usuarioId();
        e.tokenHash = t.tokenHash();
        e.expiraEm = t.expiraEm();
        e.usadoEm = t.usadoEm();
        e.criadoEm = t.criadoEm();
        return e;
    }

    public TokenAtivacao paraDominio() {
        return new TokenAtivacao(id, usuarioId, tokenHash, expiraEm, usadoEm, criadoEm);
    }
}
