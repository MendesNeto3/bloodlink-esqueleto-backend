package com.bloodlink.api.agendamento.adapter.out.messaging;

import com.bloodlink.api.agendamento.application.port.out.AgendamentoEventosPort;
import com.bloodlink.common.events.AgendamentoConfirmadoEvent;
import com.bloodlink.common.events.LembreteDoacaoEvent;
import com.bloodlink.common.log.MascaraLog;
import com.bloodlink.common.messaging.RabbitConstants;
import com.bloodlink.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitAgendamentoEventPublisherAdapter implements AgendamentoEventosPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitAgendamentoEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitAgendamentoEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void confirmado(DadosAgendamentoEvento d) {
        try {
            rabbitTemplate.convertAndSend(RabbitConstants.EXCHANGE_NOTIFICACOES,
                    RabbitConstants.ROUTING_KEY_AGENDAMENTO_CONFIRMADO,
                    new AgendamentoConfirmadoEvent(d.agendamentoId(), TenantContext.get(), d.centroNome(),
                            d.doadorId(), d.doadorNome(), d.doadorEmail(), d.dataHora()));
            log.info("Evento de confirmação publicado: agendamento {}", d.agendamentoId());
        } catch (RuntimeException e) {
            // A confirmação já foi gravada: falha de mensageria não desfaz o agendamento.
            log.error("Não foi possível publicar a confirmação do agendamento {} - causa: {}",
                    d.agendamentoId(), MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
        }
    }

    @Override
    public boolean lembrete(DadosAgendamentoEvento d) {
        try {
            rabbitTemplate.convertAndSend(RabbitConstants.EXCHANGE_NOTIFICACOES,
                    RabbitConstants.ROUTING_KEY_LEMBRETE_DOACAO,
                    new LembreteDoacaoEvent(d.agendamentoId(), TenantContext.get(), d.centroNome(),
                            d.doadorId(), d.doadorNome(), d.doadorEmail(), d.dataHora()));
            return true;
        } catch (RuntimeException e) {
            log.error("Não foi possível publicar o lembrete do agendamento {} - causa: {}",
                    d.agendamentoId(), MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            return false;
        }
    }
}
