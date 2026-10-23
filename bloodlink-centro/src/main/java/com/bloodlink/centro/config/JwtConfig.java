package com.bloodlink.centro.config;

import com.bloodlink.common.seguranca.JwtSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class JwtConfig {

    @Bean
    JwtSupport jwtSupport(@Value("${bloodlink.jwt.segredo}") String segredo,
                          @Value("${bloodlink.jwt.validade-access:15m}") Duration validadeAccess) {
        return new JwtSupport(segredo, validadeAccess);
    }
}