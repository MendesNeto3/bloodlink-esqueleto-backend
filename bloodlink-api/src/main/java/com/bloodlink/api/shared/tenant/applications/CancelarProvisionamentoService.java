package com.bloodlink.api.shared.tenant.applications;

import com.bloodlink.api.shared.tenant.application.port.out.CancelarProvisionamentoUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;
import com.bloodlink.tenant.TenantSchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Service
public class CancelarProvisionamentoService implements CancelarProvisionamentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelarProvisionamentoService.class);

    private final TenantProvisionamentoRepositoryPort repositoryPort;

    private static final Set<StatusProvisionamento> STATUS_NAO_CANCELAVEIS =
            EnumSet.of(StatusProvisionamento.CONCLUIDO);

    public CancelarProvisionamentoService(TenantProvisionamentoRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public void cancelar(String schema) {
        TenantSchemaValidator.validarFormatoProvisionamento(schema);

        TenantProvisionamento provisionamento = repositoryPort.buscarPorSchema(schema)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Provisionamento não encontrado para o schema: " + schema));

        if (provisionamento.status() == StatusProvisionamento.CANCELADO) {
            log.info("Provisionamento do schema {} já estava cancelado", schema);
            return;
        }

        validarStatusCancelavel(schema, provisionamento.status());

        repositoryPort.salvar(provisionamento.comStatus(StatusProvisionamento.CANCELADO, Instant.now()));
        log.info("Provisionamento do schema {} cancelado", schema);
    }

    private void validarStatusCancelavel(String schema, StatusProvisionamento status) {
        if (STATUS_NAO_CANCELAVEIS.contains(status)) {
            throw new IllegalStateException(
                    "Provisionamento do schema " + schema + " não pode ser cancelado no status " + status);
        }
    }
}