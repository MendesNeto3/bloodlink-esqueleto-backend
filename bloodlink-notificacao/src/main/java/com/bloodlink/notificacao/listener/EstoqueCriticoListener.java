package com.bloodlink.notificacao.listener;

import com.bloodlink.common.events.EstoqueCriticoEvent;
import com.bloodlink.common.messaging.RabbitConstants;
import com.bloodlink.notificacao.service.AlertaEstoqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EstoqueCriticoListener {

    private static final Logger log = LoggerFactory.getLogger(EstoqueCriticoListener.class);

    private final AlertaEstoqueService alertas;

    public EstoqueCriticoListener(AlertaEstoqueService alertas) {
        this.alertas = alertas;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    name = RabbitConstants.QUEUE_ESTOQUE_CRITICO,
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = ""),
                            @Argument(name = "x-dead-letter-routing-key",
                                    value = RabbitConstants.QUEUE_ESTOQUE_CRITICO_DLQ)
                    }),
            exchange = @Exchange(name = RabbitConstants.EXCHANGE_NOTIFICACOES,
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = RabbitConstants.ROUTING_KEY_ESTOQUE_CRITICO))
    public void handle(EstoqueCriticoEvent evento) {
        log.info("EstoqueCriticoEvent recebido: tipo={}, unidades={}", evento.tipoSanguineo(), evento.unidades());
        alertas.enviar(evento);
    }
}