package com.bloodlink.api.shared.tenant.applications;

import com.bloodlink.api.shared.tenant.application.port.out.ListarProvisionamentosUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.PaginaProvisionamentos;
import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;

@Service
public class ListarProvisionamentoService implements ListarProvisionamentosUseCase {

    private static final int TAMANHO_MAXIMO_LISTA = 100;

    private final TenantProvisionamentoRepositoryPort repositoryPort;

    public ListarProvisionamentoService(TenantProvisionamentoRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public PaginaProvisionamentos listar(String status, int pagina, int tamanho) {
        StatusProvisionamento statusFiltro = validarStatus(status);
        validarPaginacao(pagina, tamanho);
        return repositoryPort.listar(statusFiltro, pagina, tamanho);
    }

    private StatusProvisionamento validarStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return StatusProvisionamento.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    STR."Status inválido: \{status}. Valores aceitos: \{Arrays.toString(StatusProvisionamento.values())}");
        }
    }

    private String validarPaginacao(int pagina, int tamanho) {
        if (pagina < 0) {
            throw new IllegalArgumentException("Página inválida: " + pagina + ". Use um valor maior ou igual a 0");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO_LISTA) {
            throw new IllegalArgumentException(
                    "Tamanho inválido: " + tamanho + ". Use um valor entre 1 e " + TAMANHO_MAXIMO_LISTA);
        }
        return null;
    }
}
