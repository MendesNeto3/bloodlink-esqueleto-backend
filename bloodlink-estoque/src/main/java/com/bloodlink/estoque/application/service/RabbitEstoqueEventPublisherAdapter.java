package com.bloodlink.estoque.adapter.out.messaging;

import com.bloodlink.common.events.EstoqueCriticoEvent;
import com.bloodlink.common.log.MascaraLog;
import com.bloodlink.common.messaging.RabbitConstants;
import com.bloodlink.estoque.application.port.out.EstoqueEventosPort;
import com.bloodlink.estoque.domain.TipoSanguineo;
import com.bloodlink.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;

@Component
public class RabbitEstoqueEventPublisherAdapter implements EstoqueEventosPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitEstoqueEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    public RabbitEstoqueEventPublisherAdapter(RabbitTemplate rabbitTemplate, Clock clock) {
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
    }

    @Override
    public void estoqueCritico(TipoSanguineo tipo, long unidades, int limiarCritico) {
        // o tenant é capturado agora, ainda dentro da requisição
        EstoqueCriticoEvent evento = new EstoqueCriticoEvent(TenantContext.get(), tipo.name(), tipo.rotulo(),
                unidades, limiarCritico, clock.instant());

        Runnable publicar = () -> {
            try {
                rabbitTemplate.convertAndSend(RabbitConstants.EXCHANGE_NOTIFICACOES,
                        RabbitConstants.ROUTING_KEY_ESTOQUE_CRITICO, evento);
                log.info("Alerta de estoque crítico publicado: tipo={}", tipo.name());
            } catch (Exception e) {
                log.error("Falha ao publicar o alerta de estoque crítico ({}) - causa: {}",
                        tipo.name(), MascaraLog.causa(e));
                log.debug("Detalhe da falha de publicação", e);
            }
        };

        // só depois do commit: se a baixa for desfeita, o alerta não sai
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicar.run();
                }
            });
        } else {
            publicar.run();
        }
    }
}