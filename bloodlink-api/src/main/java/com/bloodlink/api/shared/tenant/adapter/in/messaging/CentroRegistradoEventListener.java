package com.bloodlink.api.shared.tenant.adapter.in.messaging;

import com.bloodlink.api.shared.tenant.application.port.out.ProvisionarTenantUseCase;
import com.bloodlink.common.events.CentroRegistradoEvent;
import com.bloodlink.common.messaging.RabbitConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

@Component
public class CentroRegistradoEventListener {

    private static final Logger log = LoggerFactory.getLogger(CentroRegistradoEventListener.class);

    private final ProvisionarTenantUseCase provisionarTenantUseCase;

    public CentroRegistradoEventListener(ProvisionarTenantUseCase provisionarTenantUseCase) {
        this.provisionarTenantUseCase = provisionarTenantUseCase;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    name = RabbitConstants.QUEUE_CENTRO_REGISTRADO_API,
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = ""),
                            @Argument(name = "x-dead-letter-routing-key",
                                    value = RabbitConstants.QUEUE_CENTRO_REGISTRADO_API_DLQ)
                    }),
            exchange = @Exchange(name = RabbitConstants.EXCHANGE_CENTRO_REGISTRADO,
                    type = ExchangeTypes.FANOUT, durable = "true")
    ))
    public void handle(CentroRegistradoEvent event) {
        log.info("CentroRegistradoEvent recebido: centroId={}, schema={}, ativo={}",
                event.centroId(), event.schema(), event.ativo());
        try {
            provisionarTenantUseCase.provisionar(event.centroId(), event.schema(), event.ativo());
        } catch (Exception e) {
            log.error("Falha ao provisionar schema {} a partir do evento recebido", event.schema(), e);
            throw e;
        }
    }
}