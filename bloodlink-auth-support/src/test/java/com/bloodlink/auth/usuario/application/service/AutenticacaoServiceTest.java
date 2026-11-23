package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.autenticacao.RefreshToken;
import com.bloodlink.auth.autenticacao.application.out.RefreshTokenRepositoryPort;
import com.bloodlink.auth.shared.exceptions.CredenciaisInvalidasException;
import com.bloodlink.auth.shared.exceptions.MuitasTentativasException;
import com.bloodlink.auth.shared.exceptions.OperacaoAutenticacaoException;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.web.LoginCommand;
import com.bloodlink.auth.usuario.application.out.TentativasLoginPort;
import com.bloodlink.auth.usuario.application.out.TokensEmitidos;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import com.bloodlink.auth.usuario.persistance.AreaAcesso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final String SENHA = "senhaCorreta1";

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private RefreshTokenRepositoryPort refresh;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private EmissaoDeTokens emissao;
    @Mock
    private TentativasLoginPort tentativas;

    private AutenticacaoService service;

    @BeforeEach
    void preparar() {
        when(encoder.encode(anyString())).thenReturn("hash-falso");
        service = new AutenticacaoService(usuarios, refresh, encoder, emissao, tentativas);
    }

    private Usuario equipe() {
        return new Usuario(UUID.randomUUID(), "ana@centro.com", "Ana Souza", "hash-real", Papel.ADMIN_CENTRO,
                "95b82d103312", true, AGORA, AGORA);
    }

    private Usuario doador() {
        return Usuario.doadorPendente(UUID.randomUUID(), "m@x.com", "Maria", "95b82d103312", UUID.randomUUID(), AGORA)
                .ativadoComSenha("hash-real", AGORA);
    }

    // ---------- login ----------

    @Test
    @DisplayName("login correto: e-mail normalizado, tokens emitidos e contador de falhas zerado")
    void loginComSucesso() {
        Usuario u = equipe();
        TokensEmitidos tokens = new TokensEmitidos("acesso", "refresh", 900, "Ana Souza", "ADMIN_CENTRO", "95b82d103312");
        when(tentativas.segundosDeBloqueio("ana@centro.com")).thenReturn(0L);
        when(usuarios.buscarPorEmail("ana@centro.com")).thenReturn(Optional.of(u));
        when(encoder.matches(SENHA, "hash-real")).thenReturn(true);
        when(emissao.emitir(u, "realizar o login")).thenReturn(tokens);

        TokensEmitidos r = service.login(new LoginCommand("  Ana@Centro.com ", SENHA, AreaAcesso.HEMOCENTRO));

        assertThat(r).isSameAs(tokens);
        verify(tentativas).limpar("ana@centro.com");
        verify(tentativas, never()).registrarFalha(anyString());
    }

    @Test
    @DisplayName("login sem informar a área também funciona (compatibilidade)")
    void loginSemArea() {
        Usuario u = equipe();
        when(usuarios.buscarPorEmail("ana@centro.com")).thenReturn(Optional.of(u));
        when(encoder.matches(SENHA, "hash-real")).thenReturn(true);
        when(emissao.emitir(any(), anyString())).thenReturn(new TokensEmitidos("a", "r", 900));

        assertThat(service.login(new LoginCommand("ana@centro.com", SENHA))).isNotNull();
    }

    @Test
    @DisplayName("dados ausentes são recusados antes de qualquer consulta")
    void entradaInvalida() {
        assertThatThrownBy(() -> service.login(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand(null, SENHA))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("  ", SENHA))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("a@b.com", null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("a@b.com", ""))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(usuarios, tentativas, emissao);
    }

    @Test
    @DisplayName("e-mail bloqueado por excesso de tentativas: recusado sem nem consultar o usuário")
    void bloqueado() {
        when(tentativas.segundosDeBloqueio("ana@centro.com")).thenReturn(600L);

        assertThatThrownBy(() -> service.login(new LoginCommand("ana@centro.com", SENHA)))
                .isInstanceOf(MuitasTentativasException.class);
        verifyNoInteractions(usuarios, emissao);
    }

    @Test
    @DisplayName("e-mail inexistente: gasta o mesmo tempo comparando com o hash falso e conta a falha")
    void emailInexistente() {
        when(usuarios.buscarPorEmail("ninguem@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand("ninguem@x.com", SENHA)))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha inválidos");
        verify(encoder).matches(SENHA, "hash-falso");
        verify(tentativas).registrarFalha("ninguem@x.com");
        verifyNoInteractions(emissao);
    }

    @Test
    @DisplayName("senha errada: mesma mensagem genérica e conta a falha")
    void senhaErrada() {
        when(usuarios.buscarPorEmail("ana@centro.com")).thenReturn(Optional.of(equipe()));
        when(encoder.matches("outra", "hash-real")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("ana@centro.com", "outra")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha inválidos");
        verify(tentativas).registrarFalha("ana@centro.com");
        verify(tentativas, never()).limpar(anyString());
    }

    @Test
    @DisplayName("conta pendente (sem senha) ou inativada não loga, mesmo com a senha certa")
    void contaNaoApta() {
        Usuario pendente = Usuario.doadorPendente(UUID.randomUUID(), "m@x.com", "Maria", "95b82d103312",
                UUID.randomUUID(), AGORA);
        when(usuarios.buscarPorEmail("m@x.com")).thenReturn(Optional.of(pendente));
        when(encoder.matches(SENHA, "hash-falso")).thenReturn(false);
        assertThatThrownBy(() -> service.login(new LoginCommand("m@x.com", SENHA)))
                .isInstanceOf(CredenciaisInvalidasException.class);

        Usuario inativa = equipe().inativado(AGORA);
        when(usuarios.buscarPorEmail("ana@centro.com")).thenReturn(Optional.of(inativa));
        when(encoder.matches(SENHA, "hash-real")).thenReturn(true);
        assertThatThrownBy(() -> service.login(new LoginCommand("ana@centro.com", SENHA)))
                .isInstanceOf(CredenciaisInvalidasException.class);

        verifyNoInteractions(emissao);
    }

    @Test
    @DisplayName("aba errada: doador na aba do hemocentro e equipe na aba do doador são recusados")
    void areaErrada() {
        Usuario d = doador();
        Usuario e = equipe();
        when(usuarios.buscarPorEmail("m@x.com")).thenReturn(Optional.of(d));
        when(usuarios.buscarPorEmail("ana@centro.com")).thenReturn(Optional.of(e));
        when(encoder.matches(SENHA, "hash-real")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginCommand("m@x.com", SENHA, AreaAcesso.HEMOCENTRO)))
                .isInstanceOf(CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("ana@centro.com", SENHA, AreaAcesso.DOADOR)))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verifyNoInteractions(emissao);
    }

    @Test
    @DisplayName("senha com mais de 72 bytes é recusada sem chegar ao BCrypt")
    void senhaGrandeDemais() {
        when(usuarios.buscarPorEmail("ana@centro.com")).thenReturn(Optional.of(equipe()));

        assertThatThrownBy(() -> service.login(new LoginCommand("ana@centro.com", "a1" + "x".repeat(71))))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(encoder, never()).matches(anyString(), anyString());
        verify(tentativas).registrarFalha("ana@centro.com");
    }

    @Test
    @DisplayName("falha do banco na consulta vira OperacaoAutenticacaoException")
    void falhaDoBanco() {
        when(usuarios.buscarPorEmail(anyString())).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.login(new LoginCommand("ana@centro.com", SENHA)))
                .isInstanceOf(OperacaoAutenticacaoException.class);
    }

    @Test
    @DisplayName("toString do comando de login não expõe e-mail nem senha")
    void toStringSeguro() {
        assertThat(new LoginCommand("ana@centro.com", SENHA).toString())
                .doesNotContain("ana@centro.com").doesNotContain(SENHA);
    }

    // ---------- logout ----------

    @Test
    @DisplayName("logout revoga o refresh token ativo")
    void logout() {
        UUID id = UUID.randomUUID();
        when(refresh.buscarPorHash(TokensOpacos.hash("meu-refresh")))
                .thenReturn(Optional.of(new RefreshToken(id, UUID.randomUUID(), "h", AGORA.plusSeconds(60), false, AGORA)));
        when(refresh.revogarSeAtivo(id)).thenReturn(true);

        service.logout("  meu-refresh ");

        verify(refresh).revogarSeAtivo(id);
    }

    @Test
    @DisplayName("logout com token desconhecido não falha e não revoga nada")
    void logoutDesconhecido() {
        when(refresh.buscarPorHash(anyString())).thenReturn(Optional.empty());

        service.logout("qualquer");

        verify(refresh, never()).revogarSeAtivo(any());
    }

    @Test
    @DisplayName("logout: token ausente é recusado e falha do banco vira OperacaoAutenticacaoException")
    void logoutErros() {
        assertThatThrownBy(() -> service.logout(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.logout("  ")).isInstanceOf(IllegalArgumentException.class);

        when(refresh.buscarPorHash(anyString())).thenThrow(new RuntimeException("banco fora"));
        assertThatThrownBy(() -> service.logout("x")).isInstanceOf(OperacaoAutenticacaoException.class);
    }
}
