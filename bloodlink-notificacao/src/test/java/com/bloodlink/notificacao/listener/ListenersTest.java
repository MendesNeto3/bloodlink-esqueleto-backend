package com.bloodlink.notificacao.listener;

import com.bloodlink.common.events.AgendamentoConfirmadoEvent;
import com.bloodlink.common.events.EstoqueCriticoEvent;
import com.bloodlink.common.events.LembreteDoacaoEvent;
import com.bloodlink.notificacao.service.AlertaEstoqueService;
import com.bloodlink.notificacao.service.EmailDoadorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ListenersTest {

    private static final UUID AGENDAMENTO = UUID.randomUUID();
    private static final LocalDateTime QUANDO = LocalDateTime.of(2026, 11, 18, 9, 0);

    @Mock
    private EmailDoadorService emails;
    @Mock
    private AlertaEstoqueService alertas;

    @Test
    @DisplayName("confirmação de agendamento vira e-mail de confirmação com os mesmos dados")
    void confirmado() {
        new AgendamentoConfirmadoListener(emails).handle(new AgendamentoConfirmadoEvent(
                AGENDAMENTO, "centro_95b82d103312", "Hemocentro", UUID.randomUUID(), "Maria", "m@x.com", QUANDO));

        verify(emails).enviarConfirmacao(AGENDAMENTO, "Maria", "m@x.com", "Hemocentro", QUANDO);
    }

    @Test
    @DisplayName("lembrete vira e-mail de lembrete")
    void lembrete() {
        new LembreteDoacaoListener(emails).handle(new LembreteDoacaoEvent(
                AGENDAMENTO, "centro_95b82d103312", "Hemocentro", UUID.randomUUID(), "Maria", "m@x.com", QUANDO));

        verify(emails).enviarLembrete(AGENDAMENTO, "Maria", "m@x.com", "Hemocentro", QUANDO);
    }

    @Test
    @DisplayName("estoque crítico dispara o alerta")
    void estoque() {
        EstoqueCriticoEvent e = new EstoqueCriticoEvent("centro_95b82d103312", "O_NEGATIVO", "O-", 3, 5, Instant.now());

        new EstoqueCriticoListener(alertas).handle(e);

        verify(alertas).enviar(e);
    }

    @Test
    @DisplayName("falha no envio sobe a exceção para o RabbitMQ mandar a mensagem à fila de erros (DLQ)")
    void falhaPropaga() {
        doThrow(new IllegalStateException("smtp fora")).when(emails)
                .enviarConfirmacao(AGENDAMENTO, "Maria", "m@x.com", "Hemocentro", QUANDO);

        assertThatThrownBy(() -> new AgendamentoConfirmadoListener(emails).handle(new AgendamentoConfirmadoEvent(
                AGENDAMENTO, "centro_95b82d103312", "Hemocentro", UUID.randomUUID(), "Maria", "m@x.com", QUANDO)))
                .isInstanceOf(IllegalStateException.class);
    }
}
