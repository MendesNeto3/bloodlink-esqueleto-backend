package com.bloodlink.centro.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CentroJpaRepository extends JpaRepository<CentroJpaEntity, UUID> {

    Optional<CentroJpaEntity> findBySchema(String schema);
}