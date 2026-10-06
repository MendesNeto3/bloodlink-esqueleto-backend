package com.bloodlink.api.shared.tenant.adapter.out.persistence;

import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TenantProvisionamentoRepositoryAdapter implements TenantProvisionamentoRepositoryPort {

    private final TenantProvisionamentoJpaRepository jpaRepository;

    public TenantProvisionamentoRepositoryAdapter(TenantProvisionamentoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TenantProvisionamento salvar(TenantProvisionamento provisionamento) {
        UUID id = provisionamento.id() != null ? provisionamento.id() : UUID.randomUUID();

        TenantProvisionamentoJpaEntity entity = new TenantProvisionamentoJpaEntity(
                id,
                provisionamento.centroId(),
                provisionamento.schema(),
                provisionamento.status(),
                provisionamento.iniciadoEm(),
                provisionamento.atualizadoEm(),
                provisionamento.concluidoEm()
        );

        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<TenantProvisionamento> buscarPorSchema(String schema) {
        return jpaRepository.findBySchema(schema).map(this::toDomain);
    }

    @Override
    public List<TenantProvisionamento> listarPorStatusEIniciadoAntesDe(StatusProvisionamento status, Instant limite) {
        return jpaRepository.findByStatusAndIniciadoEmBefore(status, limite)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private TenantProvisionamento toDomain(TenantProvisionamentoJpaEntity entity) {
        return new TenantProvisionamento(
                entity.getId(),
                entity.getCentroId(),
                entity.getSchema(),
                entity.getStatus(),
                entity.getIniciadoEm(),
                entity.getAtualizadoEm(),
                entity.getConcluidoEm()
        );
    }
}
