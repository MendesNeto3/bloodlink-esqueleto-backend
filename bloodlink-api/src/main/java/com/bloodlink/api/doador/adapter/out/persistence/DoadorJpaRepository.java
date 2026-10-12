package com.bloodlink.api.doador.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface DoadorJpaRepository extends
        JpaRepository<DoadorJpaEntity, UUID>, JpaSpecificationExecutor<DoadorJpaEntity> {

    Optional<DoadorJpaEntity> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
}
