package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.web.AtivarContaCommand;
import com.bloodlink.auth.usuario.application.out.TokenAtivacaoRepositoryPort;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtivarContaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final String TOKEN = "token-de-ativacao";

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private TokenAtivacaoRepositoryPort tokens;
    @Mock
    private PasswordEncoder encoder;

    private AtivarContaService service;

    @BeforeEach
    void preparar() {
        service = new AtivarContaService(usuarios, tokens, encoder, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private Usuario pendente() {
        return Usuario.doadorPendente(UUID.randomUUID(), "m@x.com", "Maria Souza", "95b82d103312",
                UUID.randomUUID(), AGORA.minusSeconds(3600));
    }

    private TokenAtivacao tokenDe(Usuario u) {
        return new TokenAtivacao(UUID.randomUUID(), u.id(), TokensOpacos.hash(TOKEN),
                AGORA.plusSeconds(600), null, AGORA.minusSeconds(60));
    }

    @Test
    @DisplayName("ativa a conta pendente com a senha escolhida e consome o token")
    void sucesso() {
        Usuario u = pendente();
        TokenAtivacao t = tokenDe(u);
        when(tokens.buscarPorHash(TokensOpacos.hash(TOKEN))).thenReturn(Optional.of(t));
        when(usuarios.buscarPorId(u.id())).thenReturn(Optional.of(u));
        when(tokens.marcarUsadoSeDisponivel(t.id(), AGORA)).thenReturn(true);
        when(encoder.encode("senhaBoa123")).thenReturn("hash-novo");

        service.ativar(new AtivarContaCommand(TOKEN, "senhaBoa123"));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).salvar(salvo.capture());
        assertThat(salvo.getValue().pendente()).isFalse();
        assertThat(salvo.getValue().senhaHash()).isEqualTo("hash-novo");
    }

    @Test
    @DisplayName("token ou senha ausentes ou fracos são recusados antes de consultar")
    void entradaInvalida() {
        assertThatThrownBy(() -> service.ativar(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.ativar(new AtivarContaCommand(" ", "senhaBoa123")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.ativar(new AtivarContaCommand(TOKEN, "fraca")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(tokens, usuarios);
    }

    @Test
    @DisplayName("token inexistente, já usado ou expirado: link inválido")
    void tokenInvalido() {
        Usuario u = pendente();
        TokenAtivacao usado = new TokenAtivacao(UUID.randomUUID(), u.id(), TokensOpacos.hash(TOKEN),
                AGORA.plusSeconds(600), AGORA.minusSeconds(1), AGORA.minusSeconds(60));
        TokenAtivacao expirado = new TokenAtivacao(UUID.randomUUID(), u.id(), TokensOpacos.hash(TOKEN),
                AGORA, null, AGORA.minusSeconds(60));
        when(tokens.buscarPorHash(TokensOpacos.hash(TOKEN)))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(usado)).thenReturn(Optional.of(expirado));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.ativar(new AtivarContaCommand(TOKEN, "senhaBoa123")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Link de ativação inválido ou expirado");
        }
        verify(usuarios, never()).salvar(any());
    }

    @Test
    @DisplayName("conta que já está ativa não é ativada de novo")
    void contaJaAtiva() {
        Usuario jaAtiva = pendente().ativadoComSenha("hash", AGORA);
        when(tokens.buscarPorHash(TokensOpacos.hash(TOKEN))).thenReturn(Optional.of(tokenDe(jaAtiva)));
        when(usuarios.buscarPorId(jaAtiva.id())).thenReturn(Optional.of(jaAtiva));

        assertThatThrownBy(() -> service.ativar(new AtivarContaCommand(TOKEN, "senhaBoa123")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarios, never()).salvar(any());
    }

    @Test
    @DisplayName("perdeu a corrida pelo token: link inválido")
    void corrida() {
        Usuario u = pendente();
        TokenAtivacao t = tokenDe(u);
        when(tokens.buscarPorHash(TokensOpacos.hash(TOKEN))).thenReturn(Optional.of(t));
        when(usuarios.buscarPorId(u.id())).thenReturn(Optional.of(u));
        when(tokens.marcarUsadoSeDisponivel(t.id(), AGORA)).thenReturn(false);

        assertThatThrownBy(() -> service.ativar(new AtivarContaCommand(TOKEN, "senhaBoa123")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarios, never()).salvar(any());
    }

    @Test
    @DisplayName("falha do banco vira OperacaoUsuarioException")
    void falhaDoBanco() {
        when(tokens.buscarPorHash(TokensOpacos.hash(TOKEN))).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.ativar(new AtivarContaCommand(TOKEN, "senhaBoa123")))
                .isInstanceOf(OperacaoUsuarioException.class);
    }
}
