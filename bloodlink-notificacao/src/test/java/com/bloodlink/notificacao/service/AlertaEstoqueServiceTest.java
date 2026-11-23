package com.bloodlink.notificacao.service;

import com.bloodlink.common.events.EstoqueCriticoEvent;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaEstoqueServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private TemplateEngine templates;

    private AlertaEstoqueService service;

    @BeforeEach
    void preparar() {
        service = new AlertaEstoqueService(mailSender, templates, "nao-responda@bloodlink.com", "gestao@hemocentro.com");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(templates.process(eq("estoque-critico"), any(Context.class))).thenReturn("<p>alerta</p>");
    }

    private EstoqueCriticoEvent evento(String schema) {
        return new EstoqueCriticoEvent(schema, "O_NEGATIVO", "O-", 3, 5, Instant.parse("2026-11-17T15:00:00Z"));
    }

    @Test
    @DisplayName("envia ao destino configurado, com assunto sem acento e o tipo sanguíneo")
    void envia() throws Exception {
        service.enviar(evento("centro_95b82d103312"));

        ArgumentCaptor<MimeMessage> c = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(c.capture());
        assertThat(c.getValue().getSubject()).isEqualTo("Alerta de estoque critico: O-");
        assertThat(((InternetAddress) c.getValue().getAllRecipients()[0]).getAddress()).isEqualTo("gestao@hemocentro.com");
        assertThat(((InternetAddress) c.getValue().getFrom()[0]).getAddress()).isEqualTo("nao-responda@bloodlink.com");
    }

    @Test
    @DisplayName("o template recebe código do centro (CTR-XXXXXX), rótulo, unidades e limiar")
    void variaveis() {
        service.enviar(evento("centro_95b82d103312"));

        ArgumentCaptor<Context> ctx = ArgumentCaptor.forClass(Context.class);
        verify(templates).process(eq("estoque-critico"), ctx.capture());
        assertThat(ctx.getValue().getVariable("centro")).isEqualTo("CTR-95B82D");
        assertThat(ctx.getValue().getVariable("rotulo")).isEqualTo("O-");
        assertThat(ctx.getValue().getVariable("unidades")).isEqualTo(3L);
        assertThat(ctx.getValue().getVariable("limiar")).isEqualTo(5);
    }

    @Test
    @DisplayName("schema ausente ou fora do padrão vira 'centro não identificado'")
    void centroNaoIdentificado() {
        for (String schema : new String[]{null, "public", "centro_abc", "outro_95b82d103312"}) {
            service.enviar(evento(schema));
        }

        ArgumentCaptor<Context> ctx = ArgumentCaptor.forClass(Context.class);
        verify(templates, org.mockito.Mockito.times(4)).process(eq("estoque-critico"), ctx.capture());
        assertThat(ctx.getAllValues()).allSatisfy(c ->
                assertThat(c.getVariable("centro")).isEqualTo("centro não identificado"));
    }
}
