package com.bloodlink.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class JwtSupport {

    private static final Logger log = LoggerFactory.getLogger(JwtSupport.class);

    private static final String CLAIM_CENTRO_ID = "centroId";
    private static final String CLAIM_PAPEL = "papel";

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
        this.key = Keys.hmacShaKeyFor(segredo.getBytes());
        this.validade = validade;
    }

    public String gerarToken(String subject, String centroId, String papel) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // jti — habilita revogação futura por id
                .issuer(ISSUER)
                .subject(subject)
                .claim(CLAIM_CENTRO_ID, centroId)
                .claim(CLAIM_PAPEL, papel)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)))
                .signWith(key)
                .compact();
    }

    /** @return o centroId da claim, ou null se o token for inválido/expirado. */
    public String validarEExtrairCentroId(String token) {
        return extrairClaim(token, CLAIM_CENTRO_ID);
    }

    /** @return o papel da claim, ou null se o token for inválido/expirado. */
    public String validarEExtrairPapel(String token) {
        return extrairClaim(token, CLAIM_PAPEL);
    }

    private String extrairClaim(String token, String nomeClaim) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clockSkewSeconds(CLOCK_SKEW_SEGUNDOS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get(nomeClaim, String.class);
        } catch (ExpiredJwtException e) {
            // Esperado no dia a dia (sessão expirou) — não indica ataque, log em debug.
            log.debug("Token expirado");
            return null;
        } catch (SignatureException e) {
            // Assinatura não bate — ou o segredo está errado em algum lugar,
            // ou é uma tentativa de forjar token. Vale WARN pra dar rastro.
            log.warn("Token com assinatura inválida recebido");
            return null;
        } catch (Exception e) {
            // Qualquer outro problema (formato corrompido, issuer errado, etc.)
            log.warn("Token inválido rejeitado: {}", e.getMessage());
            return null;
        }
    }
}