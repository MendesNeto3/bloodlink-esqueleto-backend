package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * Contador de falhas por e-mail com janela fixa: a primeira falha abre a janela (TTL); ao chegar no
 * máximo, o e-mail fica bloqueado até a janela expirar. O e-mail nunca vai para o Redis em texto
 * puro (só o hash). Se o Redis cair, o login segue sem esse controle (fail-open).
 */
@Component
public class RedisTentativasLoginAdapter implements TentativasLoginPort {

    private static final Logger log = LoggerFactory.getLogger(RedisTentativasLoginAdapter.class);
    private static final String PREFIXO = "bloodlink:auth:login:falhas:";

    private final StringRedisTemplate redis;
    private final int maxTentativas;
    private final Duration janela;

    public RedisTentativasLoginAdapter(StringRedisTemplate redis,
                                       @Value("${bloodlink.auth.login.max-tentativas:5}") int maxTentativas,
                                       @Value("${bloodlink.auth.login.bloqueio-minutos:15}") long bloqueioMinutos) {
        this.redis = redis;
        this.maxTentativas = maxTentativas;
        this.janela = Duration.ofMinutes(bloqueioMinutos);
    }

    @Override
    public long segundosDeBloqueio(String email) {
        try {
            String chave = chave(email);
            String valor = redis.opsForValue().get(chave);
            if (valor == null || Long.parseLong(valor) < maxTentativas) {
                return 0;
            }
            Long restante = redis.getExpire(chave, TimeUnit.SECONDS);
            if (restante == null || restante < 0) {
                // chave sem validade (a gravação do TTL falhou antes): recoloca, para não bloquear para sempre
                redis.expire(chave, janela);
                return janela.toSeconds();
            }
            return Math.max(1, restante);
        } catch (RuntimeException e) {
            log.warn("Controle de tentativas de login indisponível (leitura) - causa: {}", MascaraLog.causa(e));
            return 0;
        }
    }

    @Override
    public void registrarFalha(String email) {
        try {
            String chave = chave(email);
            Long total = redis.opsForValue().increment(chave);
            if (total != null && total == 1) {
                redis.expire(chave, janela);
            }
        } catch (RuntimeException e) {
            log.warn("Controle de tentativas de login indisponível (gravação) - causa: {}", MascaraLog.causa(e));
        }
    }

    @Override
    public void limpar(String email) {
        try {
            redis.delete(chave(email));
        } catch (RuntimeException e) {
            log.warn("Controle de tentativas de login indisponível (limpeza) - causa: {}", MascaraLog.causa(e));
        }
    }

    private static String chave(String email) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(email.getBytes(StandardCharsets.UTF_8));
            return PREFIXO + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
