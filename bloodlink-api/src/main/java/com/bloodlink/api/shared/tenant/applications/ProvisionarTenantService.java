package com.bloodlink.api.shared.tenant.applications;

import com.bloodlink.api.shared.tenant.application.port.out.ProvisionarTenantUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;
import com.bloodlink.tenant.SchemaProvisionerPort;
import com.bloodlink.tenant.TenantSchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProvisionarTenantService implements ProvisionarTenantUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProvisionarTenantService.class);

    private final SchemaProvisionerPort schemaProvisionerPort;
    private final TenantProvisionamentoRepositoryPort repositoryPort;

    public ProvisionarTenantService(SchemaProvisionerPort schemaProvisionerPort,
                                    TenantProvisionamentoRepositoryPort repositoryPort) {
        this.schemaProvisionerPort = schemaProvisionerPort;
        this.repositoryPort = repositoryPort;
    }

    @Override
    public void provisionar(UUID centroId, String schema, boolean ativo) {
        TenantSchemaValidator.validarFormatoProvisionamento(schema);

        if (centroId == null) {
            throw new IllegalArgumentException("centroId não pode ser nulo");
        }

        if (!ativo) {
            log.info("Centro {} inativo — provisionamento de schema {} ignorado", centroId, schema);
            return;
        }

        Instant agora = Instant.now();

        TenantProvisionamento provisionamento = repositoryPort.buscarPorSchema(schema)
                .map(existente -> existente.reiniciando(agora))
                .orElse(new TenantProvisionamento
                        (UUID.randomUUID(),
                        centroId,
                        schema,
                        StatusProvisionamento.EM_ANDAMENTO, agora, agora, null
                ));

        repositoryPort.salvar(provisionamento);

        try {
            schemaProvisionerPort.provisionar(schema);
            repositoryPort.salvar(provisionamento.comStatus(StatusProvisionamento.CONCLUIDO, Instant.now()));
            log.info("Provisionamento do schema {} concluído com sucesso", schema);
        } catch (Exception e) {
            repositoryPort.salvar(provisionamento.comStatus(StatusProvisionamento.REJEITADO, Instant.now()));
            log.error("Falha ao provisionar schema {}", schema, e);
            throw e;
        }
    }
}

