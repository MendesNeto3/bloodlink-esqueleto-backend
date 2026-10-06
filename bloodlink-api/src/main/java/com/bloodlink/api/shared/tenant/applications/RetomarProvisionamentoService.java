package com.bloodlink.api.shared.tenant.applications;

import com.bloodlink.api.shared.tenant.application.port.out.ProvisionarTenantUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.RetomarProvisionamentoUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;
import com.bloodlink.api.shared.tenant.exception.ProvisionamentoNaoEncontradoException;
import com.bloodlink.api.shared.tenant.exception.RetomadaProvisionamentoException;
import com.bloodlink.tenant.TenantSchemaValidator;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.Set;

@Service
public class RetomarProvisionamentoService implements RetomarProvisionamentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(RetomarProvisionamentoService.class);

    private static final Set<StatusProvisionamento> STATUS_NAO_RETOMAVEIS =
            EnumSet.of(StatusProvisionamento.CONCLUIDO, StatusProvisionamento.CANCELADO);

    private final TenantProvisionamentoRepositoryPort tenantProvisionamentoRepositoryPort;
    private final ProvisionarTenantUseCase tenantUseCase;

    public RetomarProvisionamentoService(TenantProvisionamentoRepositoryPort tenantProvisionamentoRepositoryPort,
                                         ProvisionarTenantUseCase tenantUseCase) {
        this.tenantProvisionamentoRepositoryPort = tenantProvisionamentoRepositoryPort;
        this.tenantUseCase = tenantUseCase;
    }

    @Override
    public void retomar(String schema) {
        TenantSchemaValidator.validarFormatoProvisionamento(schema);

        TenantProvisionamento provisionamento = tenantProvisionamentoRepositoryPort.buscarPorSchema(schema)
                .orElseThrow(() -> new ProvisionamentoNaoEncontradoException(
                        "Provisionamento não encontrado para o schema: " + schema));

        validarStatusRetomavel(schema, provisionamento.status());

        log.info("Retomando provisionamento do schema {} (status atual: {})", schema, provisionamento.status());

        try {
            tenantUseCase.provisionar(provisionamento.centroId(), schema, true);
        } catch (RuntimeException e) {
            log.error("Falha ao retomar provisionamento do schema {}", schema, e);
            throw new RetomadaProvisionamentoException(schema, e);
        }

        log.info("Provisionamento do schema {} retomado com sucesso", schema);
    }

    private void validarStatusRetomavel(String schema, StatusProvisionamento status) {
        if (STATUS_NAO_RETOMAVEIS.contains(status)) {
            throw new IllegalStateException(
                    "Provisionamento do schema " + schema + " não pode ser retomado no status " + status);
        }
    }
}
