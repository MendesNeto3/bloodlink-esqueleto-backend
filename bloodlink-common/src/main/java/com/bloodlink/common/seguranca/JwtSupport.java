package com.bloodlink.common.seguranca;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class JwtSupport {
    public record DadosToken(String subject, String centroId, String papel, String doadorId) {

        /** Compatibilidade: quem construía com 3 argumentos continua compilando. */
        public DadosToken(String subject, String centroId, String papel) {
            this(subject, centroId, papel, null);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(JwtSupport.class);

    private static final String CLAIM_CENTRO_ID = "centroId";
    private static final String CLAIM_PAPEL = "papel";
    private static final String CLAIM_DOADOR_ID = "doadorId";
    private static final String ISSUER = "bloodlink";
    private static final long CLOCK_SKEW_SEGUNDOS = 30;
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private final SecretKey key;
    private final Duration validade;

    public JwtSupport(String segredo, Duration validade) {
        if (segredo == null || segredo.length() < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalArgumentException(
                    "O segredo do JWT precisa ter pelo menos " + TAMANHO_MINIMO_SEGREDO + " caracteres");
        }
        this.key = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.validade = validade;
    }

    /** Para quem não é doador. centroId pode ser null apenas para papéis da plataforma. */
    public String gerarToken(String subject, String centroId, String papel) {
        return gerarToken(subject, centroId, papel, null);
    }

    /** doadorId só existe no papel DOADOR. */
    public String gerarToken(String subject, String centroId, String papel, String doadorId) {
        if (subject == null || subject.isBlank() || papel == null || papel.isBlank()) {
            throw new IllegalArgumentException("subject e papel são obrigatórios");
        }
        Instant agora = Instant.now();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .subject(subject)
                .claim(CLAIM_PAPEL, papel)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)));
        if (centroId != null) {
            builder.claim(CLAIM_CENTRO_ID, centroId);
        }
        if (doadorId != null) {
            builder.claim(CLAIM_DOADOR_ID, doadorId);
        }
        return builder.signWith(key).compact();
    }

    /** Vazio = token ausente, expirado, forjado ou incompleto. */
    public Optional<DadosToken> validar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clockSkewSeconds(CLOCK_SKEW_SEGUNDOS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String papel = claims.get(CLAIM_PAPEL, String.class);
            if (claims.getSubject() == null || papel == null) {
                log.warn("Token sem subject ou papel rejeitado");
                return Optional.empty();
            }
            return Optional.of(new DadosToken(
                    claims.getSubject(),
                    claims.get(CLAIM_CENTRO_ID, String.class),
                    papel,
                    claims.get(CLAIM_DOADOR_ID, String.class)));
        } catch (ExpiredJwtException e) {
            log.debug("Token expirado");
        } catch (SignatureException e) {
            log.warn("Token com assinatura inválida recebido");
        } catch (JwtException | IllegalArgumentException e) {
            // não loga e.getMessage(): pode trazer trechos do token
            log.warn("Token inválido rejeitado ({})", e.getClass().getSimpleName());
        }
        return Optional.empty();
    }
}