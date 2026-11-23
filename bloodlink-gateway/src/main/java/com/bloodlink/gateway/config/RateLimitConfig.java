package com.bloodlink.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    private static final String SEM_ORIGEM = "origem-desconhecida";

    /**
     * Limite por IP de origem. Atrás de um proxy/balanceador, o IP visto aqui é o do proxy: nesse caso
     * é preciso trocar por um resolvedor que leia o X-Forwarded-For de um proxy confiável.
     */
    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> {
            var origem = exchange.getRequest().getRemoteAddress();
            return Mono.just(origem != null && origem.getAddress() != null
                    ? origem.getAddress().getHostAddress()
                    : SEM_ORIGEM);
        };
    }
}
