package com.bloodlink.centro.config;

import com.bloodlink.common.correlation.CorrelationContext;
import com.bloodlink.common.correlation.UserRoleContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FiltrosCentroTest {

    @AfterEach
    void limpar() {
        CorrelationContext.clear();
        UserRoleContext.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("correlation id válido é mantido; inválido é trocado por um UUID novo")
    void correlation() throws Exception {
        String valido = UUID.randomUUID().toString();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Correlation-Id", valido);
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<String> noMdc = new AtomicReference<>();

        new CorrelationIdFilter().doFilter(req, res, (a, b) -> noMdc.set(MDC.get("correlationId")));

        assertThat(res.getHeader("X-Correlation-Id")).isEqualTo(valido);
        assertThat(noMdc.get()).isEqualTo(valido);
        assertThat(MDC.get("correlationId")).isNull();

        MockHttpServletRequest ruim = new MockHttpServletRequest();
        ruim.addHeader("X-Correlation-Id", "injetado");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        new CorrelationIdFilter().doFilter(ruim, res2, (a, b) -> { });
        assertThat(UUID.fromString(res2.getHeader("X-Correlation-Id"))).isNotNull();
    }

    @Test
    @DisplayName("papel conhecido entra no contexto; desconhecido ou ausente vira nulo; sempre limpa")
    void papel() throws Exception {
        AtomicReference<String> visto = new AtomicReference<>();
        UserRoleContextFilter filtro = new UserRoleContextFilter();

        MockHttpServletRequest ok = new MockHttpServletRequest();
        ok.addHeader("X-User-Role", "RECEPCAO");
        filtro.doFilter(ok, new MockHttpServletResponse(), (a, b) -> visto.set(UserRoleContext.get()));
        assertThat(visto.get()).isEqualTo("RECEPCAO");
        assertThat(UserRoleContext.get()).isNull();

        MockHttpServletRequest ruim = new MockHttpServletRequest();
        ruim.addHeader("X-User-Role", "ROOT");
        visto.set("x");
        filtro.doFilter(ruim, new MockHttpServletResponse(), (a, b) -> visto.set(UserRoleContext.get()));
        assertThat(visto.get()).isNull();

        visto.set("x");
        filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (a, b) -> visto.set(UserRoleContext.get()));
        assertThat(visto.get()).isNull();
    }

    @Test
    @DisplayName("ErrorResponse.of preenche timestamp e campos")
    void errorResponse() {
        ErrorResponse e = ErrorResponse.of(404, "Not Found", "msg", "/x");

        assertThat(e.timestamp()).isNotNull();
        assertThat(e.status()).isEqualTo(404);
        assertThat(e.message()).isEqualTo("msg");
        assertThat(e.path()).isEqualTo("/x");
    }
}
