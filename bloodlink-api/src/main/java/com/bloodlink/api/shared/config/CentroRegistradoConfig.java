package com.bloodlink.api.shared.config;

import com.bloodlink.common.messaging.RabbitConstants;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CentroRegistradoConfig {
    public static final String QUEUE_CENTRO_REGISTRADO_API_DLQ =
            RabbitConstants.QUEUE_CENTRO_REGISTRADO_API + ".dlq";

    @Bean
    Queue centroRegistradoApiDlq() {
        return QueueBuilder.durable(QUEUE_CENTRO_REGISTRADO_API_DLQ).build();
    }
}
