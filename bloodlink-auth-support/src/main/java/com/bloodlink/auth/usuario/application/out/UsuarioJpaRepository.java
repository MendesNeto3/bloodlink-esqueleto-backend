package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.usuario.Papel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, UUID>, JpaSpecificationExecutor<UsuarioJpaEntity> {
    Optional<UsuarioJpaEntity> findByEmail(String email);

    boolean existsByPapel(Papel papel);

    List<UsuarioJpaEntity> findTop2ByNomeIgnoreCase(String nome);

    List<UsuarioJpaEntity> findTop2ByNomeIgnoreCaseAndCentroId(String nome, String centroId);
}