package com.bloodlink.api.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bloodlink.common.messaging.RabbitConstants;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitConfig {

    /** Mesmo exchange que a Notificação declara; declarar nos dois lados é idempotente. */
    @Bean
    public TopicExchange notificacoesExchange() {
        return new TopicExchange(RabbitConstants.EXCHANGE_NOTIFICACOES, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
