package com.bloodlink.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitConfigTest {

    private final KeyResolver resolver = new RateLimitConfig().ipKeyResolver();

    @Test
    @DisplayName("a chave do limite é o IP de origem da requisição")
    void chavePorIp() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/x")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("10.1.2.3"), 5555)).build());

        assertThat(resolver.resolve(exchange).block()).isEqualTo("10.1.2.3");
    }

    @Test
    @DisplayName("IPs diferentes têm chaves diferentes (cada um com o seu limite)")
    void ipsDiferentes() throws Exception {
        MockServerWebExchange a = MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("10.0.0.1"), 1)).build());
        MockServerWebExchange b = MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("10.0.0.2"), 1)).build());

        assertThat(resolver.resolve(a).block()).isNotEqualTo(resolver.resolve(b).block());
    }

    @Test
    @DisplayName("sem endereço de origem usa uma chave fixa em vez de falhar")
    void semOrigem() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x").build());

        assertThat(resolver.resolve(exchange).block()).isEqualTo("origem-desconhecida");
    }

    @Test
    @DisplayName("o cabeçalho X-Forwarded-For enviado pelo cliente não influencia a chave")
    void ignoraForwardedFor() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .header("X-Forwarded-For", "1.2.3.4")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("10.9.9.9"), 1)).build());

        assertThat(resolver.resolve(exchange).block()).isEqualTo("10.9.9.9");
    }
}
