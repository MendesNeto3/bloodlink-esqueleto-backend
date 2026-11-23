package com.bloodlink.gateway.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confere o application.yml do gateway (rotas, limites, cabeçalho de serviço). Não sobe o Spring:
 * lê o arquivo como dados, então roda rápido e sem Redis.
 */
class RotasGatewayTest {

    private static Map<String, Object> gateway;
    private static List<Map<String, Object>> rotas;

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void carregar() throws Exception {
        try (InputStream in = RotasGatewayTest.class.getResourceAsStream("/application.yml")) {
            Map<String, Object> raiz = new Yaml().load(in);
            Map<String, Object> spring = (Map<String, Object>) raiz.get("spring");
            Map<String, Object> cloud = (Map<String, Object>) spring.get("cloud");
            gateway = (Map<String, Object>) cloud.get("gateway");
            rotas = (List<Map<String, Object>>) gateway.get("routes");
        }
    }

    private static Map<String, Object> rota(String id) {
        return rotas.stream().filter(r -> id.equals(r.get("id"))).findFirst()
                .orElseThrow(() -> new AssertionError("rota ausente: " + id));
    }

    private static String caminhos(Map<String, Object> rota) {
        @SuppressWarnings("unchecked")
        List<String> predicados = (List<String>) rota.get("predicates");
        return String.join(" ", predicados);
    }

    private static int posicao(String id) {
        List<Object> ids = new ArrayList<>();
        rotas.forEach(r -> ids.add(r.get("id")));
        return ids.indexOf(id);
    }

    @Test
    @DisplayName("nenhuma rota expõe /internal/** (chamadas entre serviços)")
    void semRotasInternas() {
        for (Map<String, Object> r : rotas) {
            assertThat(caminhos(r)).as(String.valueOf(r.get("id"))).doesNotContain("/internal");
        }
    }

    @Test
    @DisplayName("as rotas que o front usa existem e apontam para o serviço certo")
    void rotasEsperadas() {
        assertThat(caminhos(rota("api"))).contains("/api/agendamentos/**", "/api/doadores/**", "/api/painel/**",
                "/api/documentos/**", "/api/reservas/**", "/api/tenants/**");
        assertThat(caminhos(rota("api-publico"))).contains("/api/publico/**");
        assertThat(caminhos(rota("estoque"))).contains("/api/estoque/**");
        assertThat(caminhos(rota("checkin"))).contains("/api/checkin/**");
        assertThat(caminhos(rota("centros"))).contains("/api/centros/**");
        assertThat(caminhos(rota("auth"))).contains("/auth/**", "/api/usuarios/**");
        assertThat(String.valueOf(rota("api").get("uri"))).contains("8080");
        assertThat(String.valueOf(rota("estoque").get("uri"))).contains("8082");
        assertThat(String.valueOf(rota("checkin").get("uri"))).contains("8083");
        assertThat(String.valueOf(rota("centros").get("uri"))).contains("8084");
        assertThat(String.valueOf(rota("auth").get("uri"))).contains("8085");
    }

    @Test
    @DisplayName("rotas públicas que escrevem ou disparam e-mail têm limite de 1/s com rajada de 5")
    @SuppressWarnings("unchecked")
    void limitesApertados() {
        for (String id : new String[]{"api-publico", "auth-recuperacao"}) {
            List<Map<String, Object>> filtros = (List<Map<String, Object>>) rota(id).get("filters");
            Map<String, Object> limitador = filtros.stream()
                    .filter(f -> "RequestRateLimiter".equals(f.get("name"))).findFirst().orElseThrow();
            Map<String, Object> args = (Map<String, Object>) limitador.get("args");

            assertThat(args.get("redis-rate-limiter.replenishRate")).as(id).isEqualTo(1);
            assertThat(args.get("redis-rate-limiter.burstCapacity")).as(id).isEqualTo(5);
        }
    }

    @Test
    @DisplayName("a rota de recuperação de senha vem antes da rota geral /auth/** (a primeira que casa vence)")
    void ordemDasRotasDeAuth() {
        assertThat(caminhos(rota("auth-recuperacao"))).contains("/auth/esqueci-senha", "/auth/redefinir-senha");
        assertThat(posicao("auth-recuperacao")).isLessThan(posicao("auth"));
    }

    @Test
    @DisplayName("filtros globais: remove o cabeçalho da chave de serviço e limita 10/s por IP")
    @SuppressWarnings("unchecked")
    void filtrosGlobais() {
        List<Object> padrao = (List<Object>) gateway.get("default-filters");

        assertThat(padrao).contains("RemoveRequestHeader=X-Servico-Chave");
        Map<String, Object> limitador = (Map<String, Object>) padrao.stream()
                .filter(f -> f instanceof Map).findFirst().orElseThrow();
        Map<String, Object> args = (Map<String, Object>) limitador.get("args");
        assertThat(limitador.get("name")).isEqualTo("RequestRateLimiter");
        assertThat(args.get("redis-rate-limiter.replenishRate")).isEqualTo(10);
        assertThat(args.get("redis-rate-limiter.burstCapacity")).isEqualTo(20);
    }

    @Test
    @DisplayName("todo id de rota é único")
    void idsUnicos() {
        List<Object> ids = new ArrayList<>();
        rotas.forEach(r -> ids.add(r.get("id")));

        assertThat(ids).doesNotHaveDuplicates();
    }
}
