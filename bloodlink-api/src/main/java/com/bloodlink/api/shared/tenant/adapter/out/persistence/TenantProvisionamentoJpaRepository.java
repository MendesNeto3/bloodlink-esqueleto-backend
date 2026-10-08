package com.bloodlink.api.shared.tenant.adapter.out.persistence;

import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantProvisionamentoJpaRepository extends JpaRepository<TenantProvisionamentoJpaEntity, UUID> {
    Optional<TenantProvisionamentoJpaEntity> findBySchema(String schema);
    List<TenantProvisionamentoJpaEntity> findByStatusAndIniciadoEmBefore(StatusProvisionamento status, Instant limite);
    Page<TenantProvisionamentoJpaEntity> findByStatus(StatusProvisionamento status, Pageable pageable);
}
