package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.web.RedefinirSenhaCommand;
import com.bloodlink.auth.usuario.application.out.RevogarSessoesPort;
import com.bloodlink.auth.usuario.application.out.TentativasLoginPort;
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
class RedefinirSenhaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final String TOKEN = "token-de-teste";
    private static final String SENHA_NOVA = "novaSenha123";

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private TokenAtivacaoRepositoryPort tokens;
    @Mock
    private RevogarSessoesPort sessoes;
    @Mock
    private TentativasLoginPort tentativas;
    @Mock
    private PasswordEncoder encoder;

    private RedefinirSenhaService service;

    @BeforeEach
    void preparar() {
        service = new RedefinirSenhaService(usuarios, tokens, sessoes, tentativas, encoder,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private Usuario ativo() {
        return new Usuario(UUID.randomUUID(), "ana@centro.com", "Ana Souza", "hash-antigo", Papel.ADMIN_CENTRO,
                "95b82d103312", true, AGORA, AGORA);
    }

    private TokenAtivacao tokenDe(Usuario u, Instant expiraEm, Instant usadoEm) {
        return new TokenAtivacao(UUID.randomUUID(), u.id(), TokensOpacos.hash(TOKEN), expiraEm, usadoEm,
                AGORA.minusSeconds(60));
    }

    private RedefinirSenhaCommand comando() {
        return new RedefinirSenhaCommand(TOKEN, SENHA_NOVA);
    }

    @Test
    @DisplayName("redefine: grava a nova senha, consome o token, derruba sessões e zera o bloqueio de login")
    void sucesso() {
        Usuario u = ativo();
        TokenAtivacao t = tokenDe(u, AGORA.plusSeconds(600), null);
        when(tokens.buscarPorHash(TokensOpacos.hash(TOKEN))).thenReturn(Optional.of(t));
        when(usuarios.buscarPorId(u.id())).thenReturn(Optional.of(u));
        when(tokens.marcarUsadoSeDisponivel(t.id(), AGORA)).thenReturn(true);
        when(encoder.encode(SENHA_NOVA)).thenReturn("hash-novo");

        service.redefinir(new RedefinirSenhaCommand("  " + TOKEN + " ", SENHA_NOVA));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).salvar(salvo.capture());
        assertThat(salvo.getValue().senhaHash()).isEqualTo("hash-novo");
        verify(tokens).invalidarPendentesDoUsuario(u.id(), AGORA);
        verify(sessoes).revogarTodasDoUsuario(u.id());
        verify(tentativas).limpar("ana@centro.com");
    }

    @Test
    @DisplayName("token ou senha ausentes são recusados antes de qualquer consulta")
    void entradaInvalida() {
        assertThatThrownBy(() -> service.redefinir(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.redefinir(new RedefinirSenhaCommand(null, SENHA_NOVA)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.redefinir(new RedefinirSenhaCommand("  ", SENHA_NOVA)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.redefinir(new RedefinirSenhaCommand(TOKEN, "curta1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.redefinir(new RedefinirSenhaCommand(TOKEN, "semnumeros")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(tokens, usuarios, sessoes, tentativas);
    }

    @Test
    @DisplayName("token desconhecido, expirado ou já usado: mesma mensagem genérica, nada alterado")
    void tokenInvalido() {
        Usuario u = ativo();
        when(tokens.buscarPorHash(TokensOpacos.hash(TOKEN)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(tokenDe(u, AGORA, null)))                           // expira agora
                .thenReturn(Optional.of(tokenDe(u, AGORA.plusSeconds(600), AGORA.minusSeconds(5))));  // já usado

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.redefinir(comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Link de redefinição inválido ou expirado");
        }
        verify(usuarios, never()).salvar(any());
        verifyNoInteractions(sessoes, tentativas);
    }

    @Test
    @DisplayName("conta inexistente, pendente de ativação ou inativa: link inválido")
    void contaNaoApta() {
        Usuario pendente = Usuario.doadorPendente(UUID.randomUUID(), "m@x.com", "Maria", "95b82d103312",
                UUID.randomUUID(), AGORA);
        Usuario inativa = ativo().inativado(AGORA);
        TokenAtivacao dePendente = tokenDe(pendente, AGORA.plusSeconds(600), null);
        TokenAtivacao deInativa = tokenDe(inativa, AGORA.plusSeconds(600), null);
        TokenAtivacao deFantasma = tokenDe(ativo(), AGORA.plusSeconds(600), null);
        when(tokens.buscarPorHash(TokensOpacos.hash(TOKEN)))
                .thenReturn(Optional.of(dePendente)).thenReturn(Optional.of(deInativa)).thenReturn(Optional.of(deFantasma));
        when(usuarios.buscarPorId(pendente.id())).thenReturn(Optional.of(pendente));
        when(usuarios.buscarPorId(inativa.id())).thenReturn(Optional.of(inativa));
        when(usuarios.buscarPorId(deFantasma.usuarioId())).thenReturn(Optional.empty());

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.redefinir(comando())).isInstanceOf(IllegalArgumentException.class);
        }
        verify(tokens, never()).marcarUsadoSeDisponivel(any(), any());
        verify(usuarios, never()).salvar(any());
    }

    @Test
    @DisplayName("perdeu a corrida pelo token: link inválido e a senha não muda")
    void perdeuACorrida() {
        Usuario u = ativo();
        TokenAtivacao t = tokenDe(u, AGORA.plusSeconds(600), null);
        when(tokens.buscarPorHash(TokensOpacos.hash(TOKEN))).thenReturn(Optional.of(t));
        when(usuarios.buscarPorId(u.id())).thenReturn(Optional.of(u));
        when(tokens.marcarUsadoSeDisponivel(t.id(), AGORA)).thenReturn(false);

        assertThatThrownBy(() -> service.redefinir(comando())).isInstanceOf(IllegalArgumentException.class);
        verify(usuarios, never()).salvar(any());
        verifyNoInteractions(sessoes);
    }

    @Test
    @DisplayName("falha do banco vira OperacaoUsuarioException")
    void falhaDoBanco() {
        when(tokens.buscarPorHash(TokensOpacos.hash(TOKEN))).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.redefinir(comando())).isInstanceOf(OperacaoUsuarioException.class);
    }

    @Test
    @DisplayName("toString do comando não expõe token nem senha")
    void toStringSeguro() {
        assertThat(comando().toString()).doesNotContain(TOKEN).doesNotContain(SENHA_NOVA);
    }
}
