package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.usuario.persistance.TokenAtivacaoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenAtivacaoJpaRepository extends JpaRepository<TokenAtivacaoJpaEntity, UUID> {

    Optional<TokenAtivacaoJpaEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TokenAtivacaoJpaEntity t set t.usadoEm = :agora "
            + "where t.id = :id and t.usadoEm is null and t.expiraEm > :agora")
    int marcarUsado(@Param("id") UUID id, @Param("agora") Instant agora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TokenAtivacaoJpaEntity t set t.usadoEm = :agora "
            + "where t.usuarioId = :usuarioId and t.usadoEm is null")
    int invalidarPendentes(@Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora);
}
