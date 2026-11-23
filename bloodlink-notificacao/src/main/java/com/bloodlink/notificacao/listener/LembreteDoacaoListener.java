package com.bloodlink.notificacao.listener;

import com.bloodlink.common.events.LembreteDoacaoEvent;
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
public class LembreteDoacaoListener {

    private static final Logger log = LoggerFactory.getLogger(LembreteDoacaoListener.class);

    private final EmailDoadorService emails;

    public LembreteDoacaoListener(EmailDoadorService emails) {
        this.emails = emails;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    name = RabbitConstants.QUEUE_LEMBRETE_DOACAO,
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = ""),
                            @Argument(name = "x-dead-letter-routing-key",
                                    value = RabbitConstants.QUEUE_LEMBRETE_DOACAO_DLQ)
                    }),
            exchange = @Exchange(name = RabbitConstants.EXCHANGE_NOTIFICACOES,
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = RabbitConstants.ROUTING_KEY_LEMBRETE_DOACAO))
    public void handle(LembreteDoacaoEvent evento) {
        log.info("LembreteDoacaoEvent recebido: agendamento {}", evento.agendamentoId());
        emails.enviarLembrete(evento.agendamentoId(), evento.doadorNome(), evento.doadorEmail(),
                evento.centroNome(), evento.dataHora());
    }
}
