package com.bloodlink.api.shared.config;

import com.bloodlink.common.seguranca.JwtAuthenticationFilter;
import com.bloodlink.common.seguranca.JwtSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtSupport jwtSupport, ObjectMapper mapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(jwtSupport), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setHeader("WWW-Authenticate", "Bearer");
                            escrever(mapper, res, HttpStatus.UNAUTHORIZED,
                                    "Autenticação necessária ou token inválido", req.getRequestURI());
                        })
                        .accessDeniedHandler((req, res, ex) ->
                                escrever(mapper, res, HttpStatus.FORBIDDEN,
                                        "Acesso negado para o seu papel", req.getRequestURI())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/actuator/health", "/actuator/info").permitAll()

                        .requestMatchers("/api/tenants/provisionamentos/**").hasRole("ADMIN_PLATAFORMA")

                        // área do próprio doador (o id vem do token): doador e agendamentos dele
                        .requestMatchers("/api/doadores/me", "/api/doadores/me/**").hasRole("DOADOR")

                        // ---- agendamentos (equipe) ----
                        .requestMatchers(HttpMethod.POST, "/api/agendamentos")
                        .hasAnyRole("ADMIN_CENTRO", "RECEPCAO")
                        .requestMatchers(HttpMethod.GET, "/api/agendamentos", "/api/agendamentos/*")
                        .hasAnyRole("ADMIN_CENTRO", "RECEPCAO", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.PATCH, "/api/agendamentos/*/confirmar", "/api/agendamentos/*/cancelar")
                        .hasAnyRole("ADMIN_CENTRO", "RECEPCAO")
                        .requestMatchers(HttpMethod.PATCH, "/api/agendamentos/*/concluir")
                        .hasAnyRole("ADMIN_CENTRO", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.PATCH, "/api/agendamentos/*/faltou")
                        .hasAnyRole("ADMIN_CENTRO", "RECEPCAO", "ENFERMEIRO")

                        // ---- doadores ----
                        .requestMatchers(HttpMethod.PATCH, "/api/doadores/*/inativar", "/api/doadores/*/reativar")
                        .hasRole("ADMIN_CENTRO")

                        .requestMatchers(HttpMethod.POST, "/api/doadores/buscar")
                        .hasAnyRole("ADMIN_CENTRO", "RECEPCAO", "ENFERMEIRO")

                        .requestMatchers(HttpMethod.POST, "/api/doadores/*/doacoes")
                        .hasAnyRole("ADMIN_CENTRO", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.PATCH, "/api/doadores/*/tipo-sanguineo")
                        .hasAnyRole("ADMIN_CENTRO", "ENFERMEIRO")

                        .requestMatchers(HttpMethod.POST, "/api/doadores/*/impedimentos")
                        .hasAnyRole("ADMIN_CENTRO", "RECEPCAO", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.PATCH, "/api/doadores/*/impedimentos/*/encerrar")
                        .hasAnyRole("ADMIN_CENTRO", "RECEPCAO", "ENFERMEIRO")

                        // ficha de saúde: dado sensível, só a equipe clínica.
                        // Tem que vir ANTES das regras gerais de GET e de /api/doadores/**
                        .requestMatchers(HttpMethod.GET, "/api/doadores/*/ficha")
                        .hasAnyRole("ADMIN_CENTRO", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.PUT, "/api/doadores/*/ficha")
                        .hasAnyRole("ADMIN_CENTRO", "ENFERMEIRO")

                        .requestMatchers(HttpMethod.GET, "/api/doadores/**")
                        .hasAnyRole("ADMIN_CENTRO", "RECEPCAO", "ENFERMEIRO")

                        .requestMatchers("/api/doadores/**").hasAnyRole("ADMIN_CENTRO", "RECEPCAO")

                        .anyRequest().denyAll());
        return http.build();
    }

    private static void escrever(ObjectMapper mapper, HttpServletResponse res, HttpStatus status,
                                 String mensagem, String uri) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("status", status.value());
        corpo.put("error", mensagem);
        corpo.put("path", uri);
        mapper.writeValue(res.getOutputStream(), corpo);
    }
}