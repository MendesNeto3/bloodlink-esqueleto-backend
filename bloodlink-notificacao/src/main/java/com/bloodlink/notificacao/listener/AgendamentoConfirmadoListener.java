package com.bloodlink.notificacao.listener;

import com.bloodlink.common.events.AgendamentoConfirmadoEvent;
import com.bloodlink.common.messaging.RabbitConstants;
import com.bloodlink.notificacao.service.EmailDoadorService;
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
public class AgendamentoConfirmadoListener {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoConfirmadoListener.class);

    private final EmailDoadorService emails;

    public AgendamentoConfirmadoListener(EmailDoadorService emails) {
        this.emails = emails;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    name = RabbitConstants.QUEUE_AGENDAMENTO_CONFIRMADO,
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = ""),
                            @Argument(name = "x-dead-letter-routing-key",
                                    value = RabbitConstants.QUEUE_AGENDAMENTO_CONFIRMADO_DLQ)
                    }),
            exchange = @Exchange(name = RabbitConstants.EXCHANGE_NOTIFICACOES,
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = RabbitConstants.ROUTING_KEY_AGENDAMENTO_CONFIRMADO))
    public void handle(AgendamentoConfirmadoEvent evento) {
        log.info("AgendamentoConfirmadoEvent recebido: agendamento {}", evento.agendamentoId());
        emails.enviarConfirmacao(evento.agendamentoId(), evento.doadorNome(), evento.doadorEmail(),
                evento.centroNome(), evento.dataHora());
    }
}
