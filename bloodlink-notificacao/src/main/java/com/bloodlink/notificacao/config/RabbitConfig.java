package com.bloodlink.notificacao.config;

import com.bloodlink.common.messaging.RabbitConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /** Alertas que falharam nas 3 tentativas vão para cá, em vez de sumirem. */
    @Bean
    Queue estoqueCriticoDlq() {
        return QueueBuilder.durable(RabbitConstants.QUEUE_ESTOQUE_CRITICO_DLQ).build();
    }

    @Bean
    Queue agendamentoConfirmadoDlq() {
        return QueueBuilder.durable(RabbitConstants.QUEUE_AGENDAMENTO_CONFIRMADO_DLQ).build();
    }

    @Bean
    Queue lembreteDoacaoDlq() {
        return QueueBuilder.durable(RabbitConstants.QUEUE_LEMBRETE_DOACAO_DLQ).build();
    }
}