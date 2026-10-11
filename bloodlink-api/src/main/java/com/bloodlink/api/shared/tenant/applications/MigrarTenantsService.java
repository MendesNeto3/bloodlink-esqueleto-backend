package com.bloodlink.api.shared.tenant.applications;

import com.bloodlink.api.shared.tenant.application.port.out.MigrarTenantsUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.PaginaProvisionamentos;
import com.bloodlink.api.shared.tenant.domain.ResultadoMigracao;
import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.tenant.SchemaProvisionerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MigrarTenantsService implements MigrarTenantsUseCase {

    private static final Logger log = LoggerFactory.getLogger(MigrarTenantsService.class);
    private static final int TAMANHO_PAGINA = 100;

    private final TenantProvisionamentoRepositoryPort repository;
    private final SchemaProvisionerPort schemaProvisioner;
    private final AtomicBoolean emExecucao = new AtomicBoolean(false);

    public MigrarTenantsService(TenantProvisionamentoRepositoryPort repository, SchemaProvisionerPort schemaProvisioner) {
        this.repository = repository;
        this.schemaProvisioner = schemaProvisioner;
    }

    @Override
    public ResultadoMigracao migrarTodos() {
        if (!emExecucao.compareAndSet(false, true)) {
            throw new IllegalStateException("Já existe uma migração de tenants em andamento");
        }
        try {
            return executarMigracao();
        } finally {
            emExecucao.set(false);
        }
    }

    private ResultadoMigracao executarMigracao() {
        Map<String, Integer> migrados = new LinkedHashMap<>();
        Map<String, String> falhas = new LinkedHashMap<>();

        int pagina = 0;
        PaginaProvisionamentos resultado;
        do {
            resultado = repository.listar(StatusProvisionamento.CONCLUIDO, pagina, TAMANHO_PAGINA);
            resultado.conteudo().forEach(p -> migrarSchema(p.schema(), migrados, falhas));
            pagina++;
        } while (pagina < resultado.totalPagina());

        log.info("Migração dos tenants finalizada: {} migrados, {} com falha", migrados.size(), falhas.size());
        return new ResultadoMigracao(migrados, falhas);
    }

    private void migrarSchema(String schema, Map<String, Integer> migrados, Map<String, String> falhas) {
        try {
            int aplicadas = schemaProvisioner.migrar(schema);
            migrados.put(schema, aplicadas);
            log.info("Schema {} migrado ({} migrations aplicadas)", schema, aplicadas);
        } catch (RuntimeException e ) {
            falhas.put(schema, "Falha na migração (" + e.getClass().getSimpleName() + "); detalhes no log");
            log.error("Falha ao migrar o schema {}", schema, e);
        }
    }
}
