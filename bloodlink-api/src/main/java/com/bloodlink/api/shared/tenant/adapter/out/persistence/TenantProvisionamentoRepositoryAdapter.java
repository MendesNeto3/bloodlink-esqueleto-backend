package com.bloodlink.api.shared.tenant.adapter.out.persistence;

import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.PaginaProvisionamentos;
import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;
import com.bloodlink.api.shared.tenant.exception.ProvisionamentoJaExisteException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Pageable;
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
        UUID provisionamentoId = provisionamento.id() != null ? provisionamento.id() : UUID.randomUUID();

        TenantProvisionamentoJpaEntity entity = new TenantProvisionamentoJpaEntity(
                provisionamentoId,
                provisionamento.centroId(),
                provisionamento.schema(),
                provisionamento.status(),
                provisionamento.iniciadoEm(),
                provisionamento.atualizadoEm(),
                provisionamento.concluidoEm()
        );

        try {
            return toDomain(jpaRepository.save(entity));
        } catch (RuntimeException ex) {
            if (violouUniqueDoSchema(ex)) {
                throw new ProvisionamentoJaExisteException(provisionamento.schema(), ex);
            }
            throw ex;
        }
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

    @Override
    public PaginaProvisionamentos listar(StatusProvisionamento status, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "atualizadoEm"));

        Page<TenantProvisionamentoJpaEntity> resultado = (status == null)
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findByStatus(status, pageable);

        List<TenantProvisionamento> conteudo = resultado.getContent().stream()
                .map(this::toDomain)
                .toList();

        return new PaginaProvisionamentos(
                conteudo, resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    private TenantProvisionamento toDomain(TenantProvisionamentoJpaEntity entity) {
        return new TenantProvisionamento
                (entity.getId(),
                entity.getCentroId(),
                entity.getSchema(),
                entity.getStatus(),
                entity.getIniciadoEm(),
                entity.getAtualizadoEm(),
                entity.getConcluidoEm());
    }

    private boolean violouUniqueDoSchema(Throwable erro) {
        for (Throwable t = erro; t != null; t = t.getCause()) {
            String mensagem = t.getMessage();
            if (mensagem != null && mensagem.contains("tenant_provisionamento_schema_key")) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}

