package com.bloodlink.api.shared.tenant.applications;

import com.bloodlink.api.shared.tenant.application.port.out.ConsultarProvisionamentoUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;
import com.bloodlink.api.shared.tenant.exception.ProvisionamentoNaoEncontradoException;
import com.bloodlink.tenant.TenantSchemaValidator;
import org.springframework.stereotype.Service;

@Service
public class ConsultarProvisionamentoService implements ConsultarProvisionamentoUseCase {

    private final TenantProvisionamentoRepositoryPort repositoryPort;

    public ConsultarProvisionamentoService(TenantProvisionamentoRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public TenantProvisionamento consultar(String schema) {
        String schemaValidado = TenantSchemaValidator.validarFormatoProvisionamento(schema);
        return repositoryPort.buscarPorSchema(schemaValidado)
                .orElseThrow(()-> new ProvisionamentoNaoEncontradoException(schemaValidado));
    }
}
