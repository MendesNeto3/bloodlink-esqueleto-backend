package com.bloodlink.api.doador.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ImpedimentoJpaRepository extends JpaRepository<ImpedimentoJpaEntity, UUID> {
    List<ImpedimentoJpaEntity> findByDoadorIdOrderByCriadoEmDesc(UUID doadorId);

    List<ImpedimentoJpaEntity> findByDoadorIdInAndEncerradoEmIsNull(Collection<UUID> doadorIds);
}
