package com.bloodlink.auth.config;

import com.bloodlink.common.seguranca.JwtSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class JwtConfig {

    @Bean
    JwtSupport jwtSupport(@Value("${bloodlink.jwt.segredo}") String segredo) {
        return new JwtSupport(segredo, Duration.ofMinutes(15));
    }
}
