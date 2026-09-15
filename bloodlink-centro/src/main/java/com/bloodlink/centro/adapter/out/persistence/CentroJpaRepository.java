package com.bloodlink.centro.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CentroJpaRepository extends JpaRepository<CentroJpaEntity, UUID> {
}
