package com.bloodlink.centro.adapter.in.web;

import com.bloodlink.centro.application.port.in.CentroUseCase;
import com.bloodlink.centro.application.service.CentroNaoEncontradoException;
import com.bloodlink.centro.domain.Centro;
import com.bloodlink.centro.domain.CentroComDistancia;
import com.bloodlink.centro.domain.DiaSemana;
import com.bloodlink.centro.domain.PerfilCentro;
import com.bloodlink.centro.domain.TipoCentro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CentroControllerTest {

    private static final String CHAVE = "chave-de-servico-de-teste";

    @Mock
    private CentroUseCase useCase;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new CentroController(useCase), new CentroInternoController(useCase, CHAVE))
                .setControllerAdvice(new CentroExceptionHandler()).build();
    }

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder b, String papel) {
        return b.principal(new UsernamePasswordAuthenticationToken("u", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + papel))));
    }

    private Centro centro() {
        PerfilCentro p = new PerfilCentro(TipoCentro.FIXO, "Rua A", "Campinas", "SP",
                new BigDecimal("-22.9"), new BigDecimal("-47.06"), Set.of(DiaSemana.SEG, DiaSemana.QUA),
                LocalTime.of(8, 0), LocalTime.of(17, 0), 30);
        return new Centro(UUID.randomUUID(), "Hemocentro", "centro_95b82d103312", true, p);
    }

    @Test
    @DisplayName("POST /api/centros: 201 com id, nome e ativo; o perfil vai junto")
    void registrar() throws Exception {
        Centro c = centro();
        when(useCase.registrar(eq("Hemocentro"), any(PerfilCentro.class))).thenReturn(c);

        mvc.perform(como(post("/api/centros"), "ADMIN_PLATAFORMA").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hemocentro\",\"cidade\":\"Campinas\",\"uf\":\"SP\","
                                + "\"diasFuncionamento\":[\"SEG\",\"QUA\"],\"abertura\":\"08:00\",\"fechamento\":\"17:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Hemocentro"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.schema").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/centros: nome em branco, enum inexistente e JSON ruim viram 400 sem chamar o serviço")
    void registrarInvalido() throws Exception {
        mvc.perform(post("/api/centros").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Campo(s) inválido(s): nome"));
        mvc.perform(post("/api/centros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"tipo\":\"NAVE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/centros").contentType(MediaType.APPLICATION_JSON).content("{nao-json"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("GET /api/centros: visão da plataforma com código CTR-XXXXXX")
    void listar() throws Exception {
        when(useCase.listar()).thenReturn(List.of(centro()));

        mvc.perform(get("/api/centros")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("CTR-95B82D"))
                .andExpect(jsonPath("$[0].centroId").value("95b82d103312"))
                .andExpect(jsonPath("$[0].diasFuncionamento[0]").value("SEG"))
                .andExpect(jsonPath("$[0].abertura").value("08:00"));
    }

    @Test
    @DisplayName("GET /api/centros/publicos: sem schema nem id interno, distância arredondada em 1 casa")
    void publicos() throws Exception {
        when(useCase.listarPublicos(eq(-23.5), eq(-46.6), eq("campinas")))
                .thenReturn(List.of(new CentroComDistancia(centro(), 84.349)));

        mvc.perform(get("/api/centros/publicos").param("lat", "-23.5").param("lng", "-46.6").param("q", "campinas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distanciaKm").value(84.3))
                .andExpect(jsonPath("$[0].centroId").value("95b82d103312"))
                .andExpect(jsonPath("$[0].schema").doesNotExist())
                .andExpect(jsonPath("$[0].id").doesNotExist());

        mvc.perform(get("/api/centros/publicos").param("lat", "abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parâmetro 'lat' inválido"));
    }

    @Test
    @DisplayName("PATCH /api/centros/{id}/desativar: 200 e ativo=false")
    void desativar() throws Exception {
        Centro c = centro().desativado();
        when(useCase.desativar(c.id())).thenReturn(c);

        mvc.perform(patch("/api/centros/" + c.id() + "/desativar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(patch("/api/centros/abc/desativar")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH nome: a plataforma não tem restrição; o admin do centro é limitado ao centroId do token")
    void atualizarNome() throws Exception {
        Centro c = centro();
        when(useCase.atualizarNome(eq(c.id()), eq("Novo"), isNull())).thenReturn(c);
        when(useCase.atualizarNome(eq(c.id()), eq("Novo"), eq("95b82d103312"))).thenReturn(c);
        when(useCase.atualizarNome(eq(c.id()), eq("Novo"), eq(""))).thenThrow(new CentroNaoEncontradoException());

        mvc.perform(como(patch("/api/centros/" + c.id()), "ADMIN_PLATAFORMA").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Novo\"}")).andExpect(status().isOk());
        mvc.perform(como(patch("/api/centros/" + c.id()), "ADMIN_CENTRO").header("X-Tenant-Id", "95b82d103312")
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Novo\"}")).andExpect(status().isOk());
        // admin de centro sem o cabeçalho do token vira "" e nunca confere
        mvc.perform(como(patch("/api/centros/" + c.id()), "ADMIN_CENTRO").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Novo\"}")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH perfil: devolve a visão pública e valida o domínio (UF inválida = 400)")
    void atualizarPerfil() throws Exception {
        Centro c = centro();
        when(useCase.atualizarPerfil(eq(c.id()), any(PerfilCentro.class), isNull())).thenReturn(c);

        mvc.perform(como(patch("/api/centros/" + c.id() + "/perfil"), "ADMIN_PLATAFORMA")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cidade\":\"Campinas\",\"uf\":\"SP\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cidade").value("Campinas"));
        mvc.perform(como(patch("/api/centros/" + c.id() + "/perfil"), "ADMIN_PLATAFORMA")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cidade\":\"X\",\"uf\":\"ZZ\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("UF inválida"));
    }

    @Test
    @DisplayName("erro inesperado vira 500 genérico, sem vazar a mensagem")
    void inesperado() throws Exception {
        when(useCase.listar()).thenThrow(new RuntimeException("senha do banco"));

        mvc.perform(get("/api/centros")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Não foi possível concluir a operação no momento"));
    }

    // ---------- rota interna ----------

    @Test
    @DisplayName("GET /internal/centros/{id}: com a chave certa devolve só centroId, nome e ativo")
    void interno() throws Exception {
        when(useCase.buscarPorCentroId("95b82d103312")).thenReturn(centro());

        mvc.perform(get("/internal/centros/95b82d103312").header("X-Servico-Chave", CHAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.centroId").value("95b82d103312"))
                .andExpect(jsonPath("$.nome").value("Hemocentro"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.schema").doesNotExist());
    }

    @Test
    @DisplayName("GET /internal/centros/{id}: sem chave ou com chave errada é 403 e o serviço nem é consultado")
    void internoSemChave() throws Exception {
        mvc.perform(get("/internal/centros/95b82d103312")).andExpect(status().isForbidden());
        mvc.perform(get("/internal/centros/95b82d103312").header("X-Servico-Chave", "errada"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("Acesso negado"));
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("rota interna fica fechada para todos quando nenhuma chave está configurada")
    void internoSemChaveConfigurada() throws Exception {
        MockMvc fechado = MockMvcBuilders.standaloneSetup(new CentroInternoController(useCase, ""))
                .setControllerAdvice(new CentroExceptionHandler()).build();

        fechado.perform(get("/internal/centros/95b82d103312").header("X-Servico-Chave", ""))
                .andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("GET /internal/centros/{id}: centro inexistente é 404")
    void internoNaoEncontrado() throws Exception {
        when(useCase.buscarPorCentroId("aaaaaaaaaaaa")).thenThrow(new CentroNaoEncontradoException());

        mvc.perform(get("/internal/centros/aaaaaaaaaaaa").header("X-Servico-Chave", CHAVE))
                .andExpect(status().isNotFound());
        verify(useCase).buscarPorCentroId("aaaaaaaaaaaa");
    }
}
