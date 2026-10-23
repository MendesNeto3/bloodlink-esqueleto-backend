package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.usuario.persistance.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    /** Revoga só se ainda estiver ativo; devolve 1 se revogou e 0 se outra requisição chegou antes. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshTokenJpaEntity r set r.revogado = true where r.id = :id and r.revogado = false")
    int revogarSeAtivo(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshTokenJpaEntity r set r.revogado = true where r.usuarioId = :usuarioId and r.revogado = false")
    int revogarTodosDoUsuario(@Param("usuarioId") UUID usuarioId);
}

