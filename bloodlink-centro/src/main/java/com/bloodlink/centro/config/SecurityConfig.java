package com.bloodlink.centro.config;

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

                        .requestMatchers(HttpMethod.PATCH, "/api/centros/*/desativar")
                        .hasRole("ADMIN_PLATAFORMA")

                        .requestMatchers(HttpMethod.PATCH, "/api/centros/*")
                        .hasAnyRole("ADMIN_PLATAFORMA", "ADMIN_CENTRO")

                        .requestMatchers(HttpMethod.POST, "/api/centros").hasRole("ADMIN_PLATAFORMA")
                        .requestMatchers(HttpMethod.GET, "/api/centros", "/api/centros/**")
                        .hasRole("ADMIN_PLATAFORMA")

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