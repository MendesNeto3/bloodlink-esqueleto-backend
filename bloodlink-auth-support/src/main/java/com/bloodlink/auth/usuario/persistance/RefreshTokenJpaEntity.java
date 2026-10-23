package com.bloodlink.auth.usuario.persistance;

import com.bloodlink.auth.autenticacao.RefreshToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshTokenJpaEntity {

    @Id
    UUID id;

    @Column(name = "usuario_id", nullable = false)
    UUID usuarioId;

    @Column(name = "token_hash", nullable = false, length = 64)
    String tokenHash;

    @Column(name = "expira_em", nullable = false)
    Instant expiraEm;

    @Column(nullable = false)
    boolean revogado;

    @Column(name = "criado_em", nullable = false)
    Instant criadoEm;

    protected RefreshTokenJpaEntity() { }

    public static RefreshTokenJpaEntity de(RefreshToken t) {
        RefreshTokenJpaEntity e = new RefreshTokenJpaEntity();
        e.id = t.id();
        e.usuarioId = t.usuarioId();
        e.tokenHash = t.tokenHash();
        e.expiraEm = t.expiraEm();
        e.revogado = t.revogado();
        e.criadoEm = t.criadoEm();
        return e;
    }

    public RefreshToken paraDominio() {
        return new RefreshToken(id, usuarioId, tokenHash, expiraEm, revogado, criadoEm);
    }
}
