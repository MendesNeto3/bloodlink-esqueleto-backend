package com.bloodlink.centro.adapter.out.messaging;

import com.bloodlink.centro.application.port.out.SchemaProvisionerGateway;
import com.bloodlink.common.events.CentroRegistradoEvent;
import com.bloodlink.common.messaging.RabbitConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RabbitCentroEventPublisherAdapter implements SchemaProvisionerGateway {

    private static final Logger log = LoggerFactory.getLogger(RabbitCentroEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitCentroEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void provisionarEmtodosOsServicos(String schema) {
        Runnable publicar = () -> {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitConstants.EXCHANGE_CENTRO_REGISTRADO, "", new CentroRegistradoEvent(schema));
            } catch (Exception e) {
                // Se o broker estiver fora do ar bem na hora de publicar, o
                // centro já foi salvo no banco (isso rodou antes) — só o
                // aviso pros outros serviços é que falhou. Loga alto porque
                // isso significa provisionamento pendente sem retry
                // automático, precisa de intervenção manual.
                log.error("Falha ao publicar CentroRegistradoEvent para schema {} — provisionamento pendente!", schema, e);
            }
        };

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

