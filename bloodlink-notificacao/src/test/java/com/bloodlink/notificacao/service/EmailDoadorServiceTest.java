package com.bloodlink.notificacao.service;

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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailDoadorServiceTest {

    private static final UUID AGENDAMENTO = UUID.randomUUID();
    private static final LocalDateTime QUANDO = LocalDateTime.of(2026, 11, 18, 9, 0);

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private TemplateEngine templates;

    private EmailDoadorService service;

    @BeforeEach
    void preparar() {
        service = new EmailDoadorService(mailSender, templates, "nao-responda@bloodlink.com");
    }

    private void mensagemVazia() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    private MimeMessage enviada() {
        ArgumentCaptor<MimeMessage> c = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(c.capture());
        return c.getValue();
    }

    @Test
    @DisplayName("confirmação: usa o template certo, assunto sem acento e envia para o e-mail do doador")
    void confirmacao() throws Exception {
        mensagemVazia();
        when(templates.process(eq("agendamento-confirmado"), any(Context.class))).thenReturn("<p>ok</p>");

        service.enviarConfirmacao(AGENDAMENTO, "Maria Souza", "maria@x.com", "Hemocentro Central", QUANDO);

        MimeMessage m = enviada();
        assertThat(m.getSubject()).isEqualTo("Agendamento confirmado");
        assertThat(((InternetAddress) m.getAllRecipients()[0]).getAddress()).isEqualTo("maria@x.com");
        assertThat(((InternetAddress) m.getFrom()[0]).getAddress()).isEqualTo("nao-responda@bloodlink.com");
    }

    @Test
    @DisplayName("lembrete: usa o template de lembrete e o assunto sem acento")
    void lembrete() throws Exception {
        mensagemVazia();
        when(templates.process(eq("lembrete-doacao"), any(Context.class))).thenReturn("<p>hoje</p>");

        service.enviarLembrete(AGENDAMENTO, "Maria", "maria@x.com", "Hemocentro", QUANDO);

        assertThat(enviada().getSubject()).isEqualTo("Lembrete: sua doacao e hoje");
    }

    @Test
    @DisplayName("as variáveis do template levam nome, centro e data/hora")
    void variaveis() {
        mensagemVazia();
        when(templates.process(any(String.class), any(Context.class))).thenReturn("<p/>");

        service.enviarConfirmacao(AGENDAMENTO, "Maria Souza", "maria@x.com", "Hemocentro Central", QUANDO);

        ArgumentCaptor<Context> ctx = ArgumentCaptor.forClass(Context.class);
        verify(templates).process(eq("agendamento-confirmado"), ctx.capture());
        assertThat(ctx.getValue().getVariable("doadorNome")).isEqualTo("Maria Souza");
        assertThat(ctx.getValue().getVariable("centroNome")).isEqualTo("Hemocentro Central");
        assertThat(ctx.getValue().getVariable("dataHora")).isEqualTo(QUANDO);
    }

    @Test
    @DisplayName("nome e centro ausentes ou em branco usam textos padrão")
    void padroes() {
        mensagemVazia();
        when(templates.process(any(String.class), any(Context.class))).thenReturn("<p/>");

        service.enviarConfirmacao(AGENDAMENTO, " ", "maria@x.com", null, QUANDO);

        ArgumentCaptor<Context> ctx = ArgumentCaptor.forClass(Context.class);
        verify(templates).process(any(String.class), ctx.capture());
        assertThat(ctx.getValue().getVariable("doadorNome")).isEqualTo("doador");
        assertThat(ctx.getValue().getVariable("centroNome")).isEqualTo("seu hemocentro");
    }

    @Test
    @DisplayName("sem e-mail do doador o evento é ignorado: nada é montado nem enviado")
    void semEmail() {
        service.enviarConfirmacao(AGENDAMENTO, "Maria", null, "Hemocentro", QUANDO);
        service.enviarLembrete(AGENDAMENTO, "Maria", "  ", "Hemocentro", QUANDO);

        verifyNoInteractions(mailSender, templates);
    }

    @Test
    @DisplayName("e-mail de destino malformado: falha sem expor o endereço do doador")
    void destinoInvalido() {
        mensagemVazia();
        when(templates.process(any(String.class), any(Context.class))).thenReturn("<p/>");

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.enviarConfirmacao(AGENDAMENTO, "Maria", "isto não é e-mail <", "Hemocentro", QUANDO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("isto não é e-mail");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
