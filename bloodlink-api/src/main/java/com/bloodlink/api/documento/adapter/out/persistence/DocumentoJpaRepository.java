package com.bloodlink.api.documento.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface DocumentoJpaRepository
        extends JpaRepository<DocumentoJpaEntity, UUID>, JpaSpecificationExecutor<DocumentoJpaEntity> {

    long countByDoadorId(UUID doadorId);
}
