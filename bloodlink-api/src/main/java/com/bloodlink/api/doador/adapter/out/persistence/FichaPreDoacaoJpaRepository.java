package com.bloodlink.api.doador.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FichaPreDoacaoJpaRepository extends JpaRepository<FichaPreDoacaoJpaEntity, UUID> {
}
