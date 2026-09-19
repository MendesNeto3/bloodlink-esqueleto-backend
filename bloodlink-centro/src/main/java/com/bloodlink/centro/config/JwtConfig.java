package com.bloodlink.centro.config;

import com.bloodlink.auth.JwtSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtSupport jwtSupport(
            @Value("${BLOODLINK_JWT_SECRET:bloodlink-dev-secret-troque-em-producao-antes-de-usar}") String segredo) {
        return new JwtSupport(segredo, Duration.ofHours(8));
    }
}