package com.bloodlink.common.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!!";
    private static final String CENTRO = "95b82d103312";

    private final JwtSupport jwt = new JwtSupport(SEGREDO, Duration.ofMinutes(15));
    private final AtomicReference<HttpServletRequest> recebida = new AtomicReference<>();

    @BeforeEach
    void limparAntes() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limparDepois() {
        SecurityContextHolder.clearContext();
    }

    private void executar(JwtAuthenticationFilter filtro, MockHttpServletRequest req) throws Exception {
        filtro.doFilter(req, new MockHttpServletResponse(), (rq, rs) -> recebida.set((HttpServletRequest) rq));
    }

    private MockHttpServletRequest comToken(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        return req;
    }

    @Test
    @DisplayName("token válido de equipe: autentica e injeta tenant e papel confiáveis")
    void equipe() throws Exception {
        executar(new JwtAuthenticationFilter(jwt), comToken(jwt.gerarToken("user-1", CENTRO, "ADMIN_CENTRO")));

        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        assertThat(a).isNotNull();
        assertThat(a.getPrincipal()).isEqualTo("user-1");
        assertThat(a.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN_CENTRO");
        assertThat(recebida.get().getHeader("X-Tenant-Id")).isEqualTo(CENTRO);
        assertThat(recebida.get().getHeader("x-user-role")).isEqualTo("ADMIN_CENTRO");
        assertThat(recebida.get().getHeader("X-Doador-Id")).isNull();
    }

    @Test
    @DisplayName("cabeçalhos de contexto enviados pelo cliente são sempre descartados")
    void descartaCabecalhosDoCliente() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Tenant-Id", "aaaaaaaaaaaa");
        req.addHeader("X-User-Role", "ADMIN_PLATAFORMA");
        req.addHeader("X-Doador-Id", UUID.randomUUID().toString());
        req.addHeader("X-Outro", "fica");

        executar(new JwtAuthenticationFilter(jwt), req);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(recebida.get().getHeader("X-Tenant-Id")).isNull();
        assertThat(recebida.get().getHeader("X-User-Role")).isNull();
        assertThat(recebida.get().getHeader("X-Doador-Id")).isNull();
        assertThat(Collections.list(recebida.get().getHeaders("X-Tenant-Id"))).isEmpty();
        assertThat(Collections.list(recebida.get().getHeaderNames()))
                .contains("X-Outro").doesNotContain("X-Tenant-Id", "X-User-Role", "X-Doador-Id");
    }

    @Test
    @DisplayName("com token válido, o valor do token vence o que o cliente mandou")
    void tokenVenceCliente() throws Exception {
        MockHttpServletRequest req = comToken(jwt.gerarToken("u", CENTRO, "RECEPCAO"));
        req.addHeader("X-Tenant-Id", "ffffffffffff");
        req.addHeader("X-User-Role", "ADMIN_PLATAFORMA");

        executar(new JwtAuthenticationFilter(jwt), req);

        assertThat(recebida.get().getHeader("X-Tenant-Id")).isEqualTo(CENTRO);
        assertThat(recebida.get().getHeader("X-User-Role")).isEqualTo("RECEPCAO");
        assertThat(Collections.list(recebida.get().getHeaders("X-Tenant-Id"))).containsExactly(CENTRO);
    }

    @Test
    @DisplayName("centroId em maiúsculas é normalizado e 'bearer' minúsculo é aceito")
    void normaliza() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "bearer " + jwt.gerarToken("u", CENTRO.toUpperCase(), "ENFERMEIRO"));

        executar(new JwtAuthenticationFilter(jwt), req);

        assertThat(recebida.get().getHeader("X-Tenant-Id")).isEqualTo(CENTRO);
    }

    @Test
    @DisplayName("ADMIN_PLATAFORMA não precisa de centro")
    void plataforma() throws Exception {
        executar(new JwtAuthenticationFilter(jwt), comToken(jwt.gerarToken("admin", null, "ADMIN_PLATAFORMA")));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(recebida.get().getHeader("X-Tenant-Id")).isNull();
        assertThat(recebida.get().getHeader("X-User-Role")).isEqualTo("ADMIN_PLATAFORMA");
    }

    @Test
    @DisplayName("papel da equipe sem centro ou com centro inválido segue anônimo")
    void semCentro() throws Exception {
        executar(new JwtAuthenticationFilter(jwt), comToken(jwt.gerarToken("u", null, "ADMIN_CENTRO")));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        executar(new JwtAuthenticationFilter(jwt), comToken(jwt.gerarToken("u", "invalido", "ADMIN_CENTRO")));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(recebida.get().getHeader("X-Tenant-Id")).isNull();
    }

    @Test
    @DisplayName("DOADOR: exige doadorId em formato UUID e o repassa")
    void doador() throws Exception {
        String doadorId = UUID.randomUUID().toString();
        executar(new JwtAuthenticationFilter(jwt), comToken(jwt.gerarToken("u", CENTRO, "DOADOR", doadorId)));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(recebida.get().getHeader("X-Doador-Id")).isEqualTo(doadorId);

        SecurityContextHolder.clearContext();
        executar(new JwtAuthenticationFilter(jwt), comToken(jwt.gerarToken("u", CENTRO, "DOADOR")));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        executar(new JwtAuthenticationFilter(jwt), comToken(jwt.gerarToken("u", CENTRO, "DOADOR", "nao-e-uuid")));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("papel que não é doador com doadorId é recusado")
    void naoDoadorComDoadorId() throws Exception {
        executar(new JwtAuthenticationFilter(jwt),
                comToken(jwt.gerarToken("u", CENTRO, "RECEPCAO", UUID.randomUUID().toString())));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("papel desconhecido é recusado")
    void papelDesconhecido() throws Exception {
        executar(new JwtAuthenticationFilter(jwt), comToken(jwt.gerarToken("u", CENTRO, "HACKER")));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("token inválido, esquema diferente de Bearer ou sem cabeçalho seguem anônimos")
    void anonimo() throws Exception {
        executar(new JwtAuthenticationFilter(jwt), comToken("lixo"));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest basic = new MockHttpServletRequest();
        basic.addHeader("Authorization", "Basic abc");
        executar(new JwtAuthenticationFilter(jwt), basic);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        executar(new JwtAuthenticationFilter(jwt), new MockHttpServletRequest());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(recebida.get()).isNotNull();
    }

    @Test
    @DisplayName("fábrica de principal personalizada é usada; se rejeitar o token, segue anônimo")
    void fabrica() throws Exception {
        JwtAuthenticationFilter personalizado = new JwtAuthenticationFilter(jwt,
                (subject, papel, centro) -> subject + "|" + papel + "|" + centro);
        executar(personalizado, comToken(jwt.gerarToken("u1", CENTRO, "RECEPCAO")));
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("u1|RECEPCAO|" + CENTRO);

        SecurityContextHolder.clearContext();
        JwtAuthenticationFilter rejeita = new JwtAuthenticationFilter(jwt, (s, p, c) -> {
            throw new IllegalArgumentException("id inválido");
        });
        executar(rejeita, comToken(jwt.gerarToken("u1", CENTRO, "RECEPCAO")));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(recebida.get().getHeader("X-User-Role")).isNull();
    }
}
