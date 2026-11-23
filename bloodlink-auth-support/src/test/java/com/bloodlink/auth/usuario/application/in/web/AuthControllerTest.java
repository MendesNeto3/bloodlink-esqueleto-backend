package com.bloodlink.auth.usuario.application.in.web;

import com.bloodlink.auth.shared.exceptions.CredenciaisInvalidasException;
import com.bloodlink.auth.shared.exceptions.MuitasTentativasException;
import com.bloodlink.auth.shared.exceptions.OperacaoAutenticacaoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import com.bloodlink.auth.usuario.application.in.AtivarContaUseCase;
import com.bloodlink.auth.usuario.application.in.CriarContaDoadorUseCase;
import com.bloodlink.auth.usuario.application.in.RedefinirSenhaUseCase;
import com.bloodlink.auth.usuario.application.in.SolicitarRedefinicaoSenhaUseCase;
import com.bloodlink.auth.usuario.application.out.AutenticacaoUseCase;
import com.bloodlink.auth.usuario.application.out.RenovarTokenUseCase;
import com.bloodlink.auth.usuario.application.out.TokensEmitidos;
import com.bloodlink.auth.usuario.persistance.AreaAcesso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String CHAVE = "chave-de-servico-de-teste";

    @Mock
    private AutenticacaoUseCase autenticacao;
    @Mock
    private RenovarTokenUseCase renovar;
    @Mock
    private AtivarContaUseCase ativar;
    @Mock
    private CriarContaDoadorUseCase criarConta;
    @Mock
    private SolicitarRedefinicaoSenhaUseCase solicitar;
    @Mock
    private RedefinirSenhaUseCase redefinir;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = montar(CHAVE);
    }

    private MockMvc montar(String chave) {
        AuthController c = new AuthController(autenticacao, renovar, ativar, criarConta, solicitar, redefinir, chave);
        return MockMvcBuilders.standaloneSetup(c).setControllerAdvice(new UsuarioExceptionHandler()).build();
    }

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    // ---------- login / refresh / logout ----------

    @Test
    @DisplayName("POST /auth/login: devolve tokens, tipo Bearer, nome, papel e centro")
    void login() throws Exception {
        when(autenticacao.login(new LoginCommand("ana@centro.com", "senha123A", AreaAcesso.HEMOCENTRO)))
                .thenReturn(new TokensEmitidos("acesso", "refresh", 900, "Ana", "ADMIN_CENTRO", "95b82d103312"));

        mvc.perform(post("/auth/login").contentType(JSON)
                        .content("{\"email\":\"ana@centro.com\",\"senha\":\"senha123A\",\"area\":\"HEMOCENTRO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acesso"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.papel").value("ADMIN_CENTRO"))
                .andExpect(jsonPath("$.centroId").value("95b82d103312"));
    }

    @Test
    @DisplayName("login: credenciais inválidas 401, bloqueio 429 com Retry-After, falha interna 500 sem detalhe")
    void loginErros() throws Exception {
        when(autenticacao.login(any()))
                .thenThrow(new CredenciaisInvalidasException("E-mail ou senha inválidos"))
                .thenThrow(new MuitasTentativasException(600))
                .thenThrow(new OperacaoAutenticacaoException("Falha ao realizar o login", new RuntimeException("jdbc segredo")))
                .thenThrow(new RuntimeException("estourou"));
        String corpo = "{\"email\":\"a@b.com\",\"senha\":\"x\"}";

        mvc.perform(post("/auth/login").contentType(JSON).content(corpo)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("E-mail ou senha inválidos"));
        mvc.perform(post("/auth/login").contentType(JSON).content(corpo)).andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "600"));
        mvc.perform(post("/auth/login").contentType(JSON).content(corpo)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Falha ao realizar o login"));
        mvc.perform(post("/auth/login").contentType(JSON).content(corpo)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Erro interno inesperado"));
    }

    @Test
    @DisplayName("corpo ausente, malformado ou com área inexistente: 400")
    void corpoInvalido() throws Exception {
        mvc.perform(post("/auth/login").contentType(JSON)).andExpect(status().isBadRequest());
        mvc.perform(post("/auth/login").contentType(JSON).content("{nao-json")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Corpo da requisição ausente ou malformado"));
        mvc.perform(post("/auth/login").contentType(JSON).content("{\"email\":\"a\",\"senha\":\"b\",\"area\":\"LUA\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(autenticacao);
    }

    @Test
    @DisplayName("POST /auth/refresh e /auth/logout")
    void refreshELogout() throws Exception {
        when(renovar.renovar("r1")).thenReturn(new TokensEmitidos("novo", "r2", 900));

        mvc.perform(post("/auth/refresh").contentType(JSON).content("{\"refreshToken\":\"r1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").value("novo"));
        mvc.perform(post("/auth/logout").contentType(JSON).content("{\"refreshToken\":\"r2\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(autenticacao).logout("r2");
    }

    // ---------- ativar / esqueci / redefinir ----------

    @Test
    @DisplayName("POST /auth/ativar: 204; token inválido vira 422 com a mensagem do serviço")
    void ativar() throws Exception {
        mvc.perform(post("/auth/ativar").contentType(JSON).content("{\"token\":\"t\",\"senha\":\"senha123A\"}"))
                .andExpect(status().isNoContent());
        verify(ativar).ativar(new AtivarContaRequest("t", "senha123A").paraComando());

        doThrow(new RegraDeNegocioException("Link de ativação inválido ou expirado")).when(ativar).ativar(any());
        mvc.perform(post("/auth/ativar").contentType(JSON).content("{\"token\":\"t\",\"senha\":\"senha123A\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Link de ativação inválido ou expirado"));
    }

    @Test
    @DisplayName("POST /auth/esqueci-senha: sempre 202 e a mesma mensagem, qualquer que seja o e-mail")
    void esqueciSenha() throws Exception {
        for (String email : new String[]{"existe@x.com", "nao-existe@x.com"}) {
            mvc.perform(post("/auth/esqueci-senha").contentType(JSON).content("{\"email\":\"" + email + "\"}"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.mensagem").value(AuthController.MENSAGEM_ESQUECI_SENHA));
            verify(solicitar).solicitar(email);
        }
    }

    @Test
    @DisplayName("esqueci-senha: e-mail inválido vira 400; corpo ausente 400")
    void esqueciSenhaInvalido() throws Exception {
        doThrow(new IllegalArgumentException("E-mail inválido")).when(solicitar).solicitar("lixo");

        mvc.perform(post("/auth/esqueci-senha").contentType(JSON).content("{\"email\":\"lixo\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("E-mail inválido"));
        mvc.perform(post("/auth/esqueci-senha").contentType(JSON)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/redefinir-senha: 204 repassando token e senha; link inválido 422")
    void redefinirSenha() throws Exception {
        mvc.perform(post("/auth/redefinir-senha").contentType(JSON).content("{\"token\":\"t\",\"senha\":\"novaSenha1\"}"))
                .andExpect(status().isNoContent());
        verify(redefinir).redefinir(new RedefinirSenhaCommand("t", "novaSenha1"));

        doThrow(new RegraDeNegocioException("Link de redefinição inválido ou expirado")).when(redefinir).redefinir(any());
        mvc.perform(post("/auth/redefinir-senha").contentType(JSON).content("{\"token\":\"x\",\"senha\":\"novaSenha1\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- rota interna ----------

    @Test
    @DisplayName("POST /internal/contas-doador: com a chave certa cria a conta e devolve o id")
    void contaDoador() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID doadorId = UUID.randomUUID();
        when(criarConta.criar(new CriarContaDoadorRequest("Maria", "m@x.com", "95b82d103312", doadorId).paraComando()))
                .thenReturn(usuarioId);

        mvc.perform(post("/internal/contas-doador").header("X-Servico-Chave", CHAVE).contentType(JSON)
                        .content("{\"nome\":\"Maria\",\"email\":\"m@x.com\",\"centroId\":\"95b82d103312\",\"doadorId\":\""
                                + doadorId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(usuarioId.toString()));
    }

    @Test
    @DisplayName("rota interna: sem chave, com chave errada ou sem chave configurada é 403 e nada é criado")
    void contaDoadorSemChave() throws Exception {
        String corpo = "{\"nome\":\"Maria\",\"email\":\"m@x.com\",\"centroId\":\"95b82d103312\"}";

        mvc.perform(post("/internal/contas-doador").contentType(JSON).content(corpo)).andExpect(status().isForbidden());
        mvc.perform(post("/internal/contas-doador").header("X-Servico-Chave", "errada").contentType(JSON).content(corpo))
                .andExpect(status().isForbidden());
        montar("").perform(post("/internal/contas-doador").header("X-Servico-Chave", "").contentType(JSON).content(corpo))
                .andExpect(status().isForbidden());
        verifyNoInteractions(criarConta);
    }

    @Test
    @DisplayName("os toString dos pedidos não expõem senha, token nem e-mail")
    void toStringsSeguros() {
        assertThat(new LoginRequest("a@b.com", "segredo123", null).toString()).doesNotContain("segredo123").doesNotContain("a@b.com");
        assertThat(new RefreshRequest("token-secreto").toString()).doesNotContain("token-secreto");
        assertThat(new AtivarContaRequest("tok", "segredo123").toString()).doesNotContain("tok").doesNotContain("segredo123");
        assertThat(new RedefinirSenhaRequest("tok", "segredo123").toString()).doesNotContain("segredo123");
        assertThat(new EsqueciSenhaRequest("a@b.com").toString()).doesNotContain("a@b.com");
        assertThat(new TokensEmitidos("acesso-secreto", "refresh-secreto", 900).toString())
                .doesNotContain("acesso-secreto").doesNotContain("refresh-secreto");
    }
}
