package com.bloodlink.notificacao.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/** Monta e envia os e-mails que o doador recebe sobre o agendamento dele. */
@Service
public class EmailDoadorService {

    private static final Logger log = LoggerFactory.getLogger(EmailDoadorService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String remetente;

    public EmailDoadorService(JavaMailSender mailSender, TemplateEngine templateEngine,
                              @Value("${bloodlink.notificacao.remetente}") String remetente) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.remetente = remetente;
    }

    public void enviarConfirmacao(UUID agendamentoId, String doadorNome, String doadorEmail,
                                  String centroNome, LocalDateTime dataHora) {
        enviar("agendamento-confirmado", "Agendamento confirmado", agendamentoId, doadorNome, doadorEmail,
                centroNome, dataHora);
    }

    public void enviarLembrete(UUID agendamentoId, String doadorNome, String doadorEmail,
                               String centroNome, LocalDateTime dataHora) {
        enviar("lembrete-doacao", "Lembrete: sua doacao e hoje", agendamentoId, doadorNome, doadorEmail,
                centroNome, dataHora);
    }

    // O assunto não leva acento de propósito (mesmo padrão do alerta de estoque).
    private void enviar(String template, String assunto, UUID agendamentoId, String doadorNome, String doadorEmail,
                        String centroNome, LocalDateTime dataHora) {
        if (doadorEmail == null || doadorEmail.isBlank()) {
            log.warn("Evento do agendamento {} sem e-mail do doador: ignorado", agendamentoId);
            return;
        }
        Context contexto = new Context(Locale.forLanguageTag("pt-BR"));
        contexto.setVariable("doadorNome", doadorNome != null && !doadorNome.isBlank() ? doadorNome : "doador");
        contexto.setVariable("centroNome", centroNome != null && !centroNome.isBlank() ? centroNome : "seu hemocentro");
        contexto.setVariable("dataHora", dataHora);
        String html = templateEngine.process(template, contexto);

        MimeMessage mensagem = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(doadorEmail);
            helper.setSubject(assunto);
            helper.setText(html, true);
        } catch (MessagingException e) {
            // Mensagem sem dados pessoais: o e-mail do doador não vai para a exceção nem para o log.
            throw new IllegalStateException("Falha ao montar o e-mail '" + template + "' do agendamento " + agendamentoId);
        }
        mailSender.send(mensagem);
        log.info("E-mail '{}' enviado: agendamento {}", template, agendamentoId);
    }
}
