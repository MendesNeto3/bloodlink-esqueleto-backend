package com.bloodlink.centro.adapter.out.messaging;

import com.bloodlink.common.events.CentroRegistradoEvent;
import com.bloodlink.common.messaging.RabbitConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitCentroEventPublisherAdapterTest {

    @Mock
    private RabbitTemplate rabbit;

    @AfterEach
    void limpar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("sem transação publica na hora o evento de centro registrado")
    void semTransacao() {
        UUID id = UUID.randomUUID();

        new RabbitCentroEventPublisherAdapter(rabbit).provisionarEmtodosOsServicos(id, "centro_95b82d103312", true);

        verify(rabbit).convertAndSend(eq(RabbitConstants.EXCHANGE_CENTRO_REGISTRADO), eq(""), any(CentroRegistradoEvent.class));
    }

    @Test
    @DisplayName("dentro de transação só publica depois do commit")
    void comTransacao() {
        TransactionSynchronizationManager.initSynchronization();

        new RabbitCentroEventPublisherAdapter(rabbit)
                .provisionarEmtodosOsServicos(UUID.randomUUID(), "centro_95b82d103312", true);

        verify(rabbit, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(rabbit).convertAndSend(eq(RabbitConstants.EXCHANGE_CENTRO_REGISTRADO), eq(""), any(CentroRegistradoEvent.class));
    }

    @Test
    @DisplayName("falha do RabbitMQ não derruba o cadastro do centro")
    void falhaEngolida() {
        doThrow(new RuntimeException("rabbit fora")).when(rabbit)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertThatCode(() -> new RabbitCentroEventPublisherAdapter(rabbit)
                .provisionarEmtodosOsServicos(UUID.randomUUID(), "centro_95b82d103312", true))
                .doesNotThrowAnyException();
    }
}
