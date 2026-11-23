package com.bloodlink.auth.usuario.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokensOpacosETokenAtivacaoTest {

    @Test
    @DisplayName("token gerado tem 256 bits em Base64 sem preenchimento e não se repete")
    void gerar() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String t = TokensOpacos.gerar();
            assertThat(t).hasSize(43).matches("[A-Za-z0-9_-]+");
            vistos.add(t);
        }
        assertThat(vistos).hasSize(200);
    }

    @Test
    @DisplayName("hash é SHA-256 em hexadecimal, determinístico, e não devolve o token")
    void hash() {
        assertThat(TokensOpacos.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(TokensOpacos.hash("abc")).isEqualTo(TokensOpacos.hash("abc"));
        assertThat(TokensOpacos.hash("abc")).isNotEqualTo(TokensOpacos.hash("abd"));
    }

    @Test
    @DisplayName("TokenAtivacao expira exatamente no instante de expiração")
    void expiracao() {
        Instant expira = Instant.parse("2026-11-17T16:00:00Z");
        TokenAtivacao t = new TokenAtivacao(UUID.randomUUID(), UUID.randomUUID(), "h", expira, null, expira.minusSeconds(60));

        assertThat(t.expirado(expira.minusSeconds(1))).isFalse();
        assertThat(t.expirado(expira)).isTrue();
        assertThat(t.expirado(expira.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("toString do token não expõe o hash")
    void toStringSeguro() {
        TokenAtivacao t = new TokenAtivacao(UUID.randomUUID(), UUID.randomUUID(), "hash-secreto",
                Instant.EPOCH, null, Instant.EPOCH);

        assertThat(t.toString()).doesNotContain("hash-secreto");
    }
}
