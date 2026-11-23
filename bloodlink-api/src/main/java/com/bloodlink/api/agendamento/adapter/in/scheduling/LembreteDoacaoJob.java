package com.bloodlink.api.agendamento.adapter.in.scheduling;

import com.bloodlink.api.agendamento.application.usecase.LembreteDoacaoUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.PaginaProvisionamentos;
import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.common.log.MascaraLog;
import com.bloodlink.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Todo dia de manhã, envia o lembrete dos agendamentos confirmados de hoje, tenant por tenant. */
@Component
public class LembreteDoacaoJob {

    private static final Logger log = LoggerFactory.getLogger(LembreteDoacaoJob.class);
    private static final int TAMANHO_PAGINA = 100;

    private final TenantProvisionamentoRepositoryPort provisionamentos;
    private final LembreteDoacaoUseCase lembretes;

    public LembreteDoacaoJob(TenantProvisionamentoRepositoryPort provisionamentos, LembreteDoacaoUseCase lembretes) {
        this.provisionamentos = provisionamentos;
        this.lembretes = lembretes;
    }

    @Scheduled(cron = "${bloodlink.agendamento.lembrete-cron:0 0 7 * * *}", zone = "America/Sao_Paulo")
    public void enviarLembretes() {
        int pagina = 0;
        PaginaProvisionamentos resultado;
        do {
            resultado = provisionamentos.listar(StatusProvisionamento.CONCLUIDO, pagina, TAMANHO_PAGINA);
            resultado.conteudo().forEach(p -> enviarNoTenant(p.schema()));
            pagina++;
        } while (pagina < resultado.totalPagina());
    }

    private void enviarNoTenant(String schema) {
        // Um tenant com problema não pode impedir os lembretes dos outros.
        try {
            TenantContext.set(schema);
            lembretes.enviarLembretesDeHoje();
        } catch (RuntimeException e) {
            log.error("Falha nos lembretes do schema {} - causa: {}", schema, MascaraLog.causa(e));
        } finally {
            TenantContext.clear();
        }
    }
}
