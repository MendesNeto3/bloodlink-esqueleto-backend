package com.bloodlink.api.reserva.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservaJpaRepository extends JpaRepository<ReservaJpaEntity, UUID> {

    List<ReservaJpaEntity> findTop500ByOrderByCriadoEmDesc();
}
