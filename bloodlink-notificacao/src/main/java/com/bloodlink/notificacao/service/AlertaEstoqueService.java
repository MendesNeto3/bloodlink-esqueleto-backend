package com.bloodlink.notificacao.service;

import com.bloodlink.common.events.EstoqueCriticoEvent;
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

import java.util.Locale;

@Service
public class AlertaEstoqueService {

    private static final Logger log = LoggerFactory.getLogger(AlertaEstoqueService.class);
    private static final String PREFIXO_SCHEMA = "centro_";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String remetente;
    private final String destino;

    public AlertaEstoqueService(JavaMailSender mailSender, TemplateEngine templateEngine,
                                @Value("${bloodlink.notificacao.remetente}") String remetente,
                                @Value("${bloodlink.notificacao.alerta-estoque.destino}") String destino) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.remetente = remetente;
        this.destino = destino;
    }

    public void enviar(EstoqueCriticoEvent evento) {
        Context contexto = new Context(Locale.forLanguageTag("pt-BR"));
        contexto.setVariable("centro", codigoDoCentro(evento.centroSchema()));
        contexto.setVariable("rotulo", evento.rotulo());
        contexto.setVariable("unidades", evento.unidades());
        contexto.setVariable("limiar", evento.limiarCritico());
        String html = templateEngine.process("estoque-critico", contexto);

        MimeMessage mensagem = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(destino);
            helper.setSubject("Alerta de estoque critico: " + evento.rotulo()); // sem acento de propósito
            helper.setText(html, true);
        } catch (MessagingException e) {
            throw new IllegalStateException("Falha ao montar o e-mail do alerta de estoque", e);
        }
        mailSender.send(mensagem);
        log.info("Alerta de estoque crítico enviado: tipo={}", evento.tipoSanguineo());
    }

    /** centro_a1b2c3d4e5f6 vira CTR-A1B2C3, o mesmo código que a tela de centros mostra. */
    private static String codigoDoCentro(String schema) {
        if (schema == null || !schema.startsWith(PREFIXO_SCHEMA) || schema.length() < PREFIXO_SCHEMA.length() + 6) {
            return "centro não identificado";
        }
        return "CTR-" + schema.substring(PREFIXO_SCHEMA.length(), PREFIXO_SCHEMA.length() + 6).toUpperCase(Locale.ROOT);
    }
}