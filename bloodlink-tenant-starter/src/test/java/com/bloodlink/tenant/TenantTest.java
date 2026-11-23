package com.bloodlink.tenant;

import com.bloodlink.common.correlation.CorrelationContext;
import com.bloodlink.common.correlation.UserRoleContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTest {

    @AfterEach
    void limpar() {
        TenantContext.clear();
        UserRoleContext.clear();
        CorrelationContext.clear();
        MDC.clear();
    }

    // ---------- TenantContext / resolver ----------

    @Test
    @DisplayName("sem tenant definido o contexto é o schema public; set/clear funcionam")
    void contexto() {
        assertThat(TenantContext.get()).isEqualTo("public");

        TenantContext.set("centro_95b82d103312");
        assertThat(TenantContext.get()).isEqualTo("centro_95b82d103312");
        assertThat(new TenantIdentifierResolver().resolveCurrentTenantIdentifier()).isEqualTo("centro_95b82d103312");
        assertThat(new TenantIdentifierResolver().validateExistingCurrentSessions()).isTrue();

        TenantContext.clear();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.SCHEMA_PUBLICO);
    }

    // ---------- TenantSchemaValidator ----------

    @Test
    @DisplayName("validarFormato aceita public e centro_<12 hex>; recusa o resto (anti SQL injection)")
    void validarFormato() {
        TenantSchemaValidator.validarFormato("public");
        TenantSchemaValidator.validarFormato("centro_95b82d103312");
        for (String ruim : new String[]{null, "", "PUBLIC", "centro_95B82D103312", "centro_95b82d10331",
                "centro_95b82d1033123", "centro_95b82d10331z", "public; drop schema x", "centro_95b82d103312\"",
                "otro_95b82d103312"}) {
            assertThatThrownBy(() -> TenantSchemaValidator.validarFormato(ruim))
                    .as(String.valueOf(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("tenant e provisionamento não aceitam o schema public")
    void validarTenant() {
        TenantSchemaValidator.validarFormatoTenant("centro_95b82d103312", "95b82d103312");
        assertThat(TenantSchemaValidator.validarFormatoProvisionamento("centro_95b82d103312"))
                .isEqualTo("centro_95b82d103312");

        assertThatThrownBy(() -> TenantSchemaValidator.validarFormatoTenant("public", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TenantSchemaValidator.validarFormatoTenant(null, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TenantSchemaValidator.validarFormatoProvisionamento("public"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TenantSchemaValidator.validarFormatoProvisionamento(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- TenantResolverFilter ----------

    private final AtomicReference<String> schemaNaChain = new AtomicReference<>();

    private MockHttpServletResponse filtrar(MockHttpServletRequest req) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        new TenantResolverFilter().doFilter(req, res, (rq, rs) -> schemaNaChain.set(TenantContext.get()));
        return res;
    }

    private MockHttpServletRequest requisicao(String uri, String tenant) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setRequestURI(uri);
        if (tenant != null) {
            req.addHeader("X-Tenant-Id", tenant);
        }
        return req;
    }

    @Test
    @DisplayName("tenant válido: define o schema durante a requisição e limpa no fim")
    void resolverValido() throws Exception {
        MockHttpServletResponse res = filtrar(requisicao("/api/doadores", " 95b82d103312 "));

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(schemaNaChain.get()).isEqualTo("centro_95b82d103312");
        assertThat(TenantContext.get()).isEqualTo("public");
    }

    @Test
    @DisplayName("tenant ausente, em maiúsculas ou malformado: 400 e a chain não é chamada")
    void resolverInvalido() throws Exception {
        for (String ruim : new String[]{null, "", "95B82D103312", "abc", "95b82d103312x", "centro_95b82d103312"}) {
            schemaNaChain.set(null);
            MockHttpServletResponse res = filtrar(requisicao("/api/doadores", ruim));

            assertThat(res.getStatus()).as(String.valueOf(ruim)).isEqualTo(400);
            assertThat(res.getContentAsString()).contains("\"status\":400").contains("/api/doadores");
            assertThat(schemaNaChain.get()).isNull();
        }
    }

    @Test
    @DisplayName("rotas de plataforma, actuator e públicas passam sem tenant")
    void rotasSemTenant() throws Exception {
        for (String uri : new String[]{"/api/tenants/provisionamentos", "/actuator/health", "/api/publico/centros"}) {
            schemaNaChain.set(null);
            MockHttpServletResponse res = filtrar(requisicao(uri, null));

            assertThat(res.getStatus()).as(uri).isEqualTo(200);
            assertThat(schemaNaChain.get()).isEqualTo("public");
        }
    }

    @Test
    @DisplayName("o caminho aparece escapado no JSON de erro")
    void caminhoEscapado() throws Exception {
        MockHttpServletResponse res = filtrar(requisicao("/api/x\"y", null));

        assertThat(res.getContentAsString()).contains("/api/x\\\"y");
    }

    @Test
    @DisplayName("o schema é limpo mesmo se a chain lançar exceção")
    void limpaNoErro() {
        assertThatThrownBy(() -> new TenantResolverFilter().doFilter(requisicao("/api/doadores", "95b82d103312"),
                new MockHttpServletResponse(), (rq, rs) -> { throw new RuntimeException("falha"); }))
                .isInstanceOf(RuntimeException.class);

        assertThat(TenantContext.get()).isEqualTo("public");
    }

    // ---------- UserRoleContextFilter ----------

    @Test
    @DisplayName("papel conhecido entra no contexto; desconhecido ou ausente vira nulo; limpa no fim")
    void papelFiltro() throws Exception {
        AtomicReference<String> visto = new AtomicReference<>();
        UserRoleContextFilter filtro = new UserRoleContextFilter();

        MockHttpServletRequest comPapel = new MockHttpServletRequest();
        comPapel.addHeader("X-User-Role", "ADMIN_CENTRO");
        filtro.doFilter(comPapel, new MockHttpServletResponse(), (rq, rs) -> visto.set(UserRoleContext.get()));
        assertThat(visto.get()).isEqualTo("ADMIN_CENTRO");
        assertThat(UserRoleContext.get()).isNull();

        MockHttpServletRequest desconhecido = new MockHttpServletRequest();
        desconhecido.addHeader("X-User-Role", "HACKER");
        visto.set("x");
        filtro.doFilter(desconhecido, new MockHttpServletResponse(), (rq, rs) -> visto.set(UserRoleContext.get()));
        assertThat(visto.get()).isNull();

        visto.set("x");
        filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (rq, rs) -> visto.set(UserRoleContext.get()));
        assertThat(visto.get()).isNull();
    }

    // ---------- CorrelationIdFilter ----------

    @Test
    @DisplayName("correlation id: UUID válido é mantido; ausente ou inválido é substituído; vai para a resposta e o MDC")
    void correlationFiltro() throws Exception {
        CorrelationIdFilter filtro = new CorrelationIdFilter();
        AtomicReference<String> noContexto = new AtomicReference<>();
        AtomicReference<String> noMdc = new AtomicReference<>();
        String valido = UUID.randomUUID().toString();

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Correlation-Id", valido);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(req, res, (rq, rs) -> {
            noContexto.set(CorrelationContext.get());
            noMdc.set(MDC.get("correlationId"));
        });
        assertThat(noContexto.get()).isEqualTo(valido);
        assertThat(noMdc.get()).isEqualTo(valido);
        assertThat(res.getHeader("X-Correlation-Id")).isEqualTo(valido);
        assertThat(MDC.get("correlationId")).isNull();

        for (String ruim : new String[]{null, "injetado\r\nX-Evil: 1", "nao-e-uuid"}) {
            MockHttpServletRequest r = new MockHttpServletRequest();
            if (ruim != null) {
                r.addHeader("X-Correlation-Id", ruim);
            }
            MockHttpServletResponse rs2 = new MockHttpServletResponse();
            filtro.doFilter(r, rs2, (a, b) -> { });
            String gerado = rs2.getHeader("X-Correlation-Id");
            assertThat(gerado).isNotEqualTo(ruim);
            assertThat(UUID.fromString(gerado)).isNotNull();
        }
    }
}
