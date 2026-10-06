package com.bloodlink.api.shared.tenant.application.port.out;


import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class TenantProvisionamentoTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisionamentoTimeoutJob.class);
    private static final long TIMEOUT_HORAS = 12;

    private final TenantProvisionamentoRepositoryPort repositoryPort;

    public TenantProvisionamentoTimeoutJob(TenantProvisionamentoRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cancelarProvisionamentosAbandonados() {
        Instant limite = Instant.now().minus(TIMEOUT_HORAS, ChronoUnit.HOURS);

        List<TenantProvisionamento> abandonados =
                repositoryPort.listarPorStatusEIniciadoAntesDe(StatusProvisionamento.EM_ANDAMENTO, limite);

        if (abandonados.isEmpty()) {
            return;
        }

        log.warn("Encontrados {} provisionamentos abandonados (>{}h), cancelando...", abandonados.size(), TIMEOUT_HORAS);

        for (TenantProvisionamento provisionamento : abandonados) {
            repositoryPort.salvar(provisionamento.comStatus(StatusProvisionamento.CANCELADO, Instant.now()));
            log.warn("Provisionamento cancelado por timeout: schema={}", provisionamento.schema());
        }
    }
}

