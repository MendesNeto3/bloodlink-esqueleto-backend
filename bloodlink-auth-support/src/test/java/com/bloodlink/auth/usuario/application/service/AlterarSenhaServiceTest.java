package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.AlterarSenhaCommand;
import com.bloodlink.auth.usuario.application.out.RevogarSessoesPort;
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
class AlterarSenhaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private RevogarSessoesPort sessoes;
    @Mock
    private PasswordEncoder encoder;

    private AlterarSenhaService service;

    @BeforeEach
    void preparar() {
        service = new AlterarSenhaService(usuarios, sessoes, encoder, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private Usuario usuario() {
        return new Usuario(UUID.randomUUID(), "ana@centro.com", "Ana Souza", "hash-atual", Papel.ADMIN_CENTRO,
                "95b82d103312", true, AGORA, AGORA);
    }

    private Solicitante solicitante(Usuario u) {
        return new Solicitante(u.id(), u.papel(), u.centroId());
    }

    @Test
    @DisplayName("troca a senha, grava o novo hash e derruba todas as sessões")
    void sucesso() {
        Usuario u = usuario();
        when(usuarios.buscarPorId(u.id())).thenReturn(Optional.of(u));
        when(encoder.matches("senhaAtual1", "hash-atual")).thenReturn(true);
        when(encoder.matches("novaSenha123", "hash-atual")).thenReturn(false);
        when(encoder.encode("novaSenha123")).thenReturn("hash-novo");

        service.alterar(new AlterarSenhaCommand("senhaAtual1", "novaSenha123"), solicitante(u));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).salvar(salvo.capture());
        assertThat(salvo.getValue().senhaHash()).isEqualTo("hash-novo");
        verify(sessoes).revogarTodasDoUsuario(u.id());
    }

    @Test
    @DisplayName("dados ausentes ou nova senha fraca são recusados antes de consultar")
    void entradaInvalida() {
        Usuario u = usuario();
        assertThatThrownBy(() -> service.alterar(null, solicitante(u))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.alterar(new AlterarSenhaCommand("a", "novaSenha123"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.alterar(new AlterarSenhaCommand(null, "novaSenha123"), solicitante(u)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.alterar(new AlterarSenhaCommand("", "novaSenha123"), solicitante(u)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.alterar(new AlterarSenhaCommand("senhaAtual1", "curta1"), solicitante(u)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(usuarios, sessoes);
    }

    @Test
    @DisplayName("usuário inexistente: não encontrado")
    void inexistente() {
        Usuario u = usuario();
        when(usuarios.buscarPorId(u.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.alterar(new AlterarSenhaCommand("senhaAtual1", "novaSenha123"), solicitante(u)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("usuário inativo não troca senha")
    void inativo() {
        Usuario u = usuario().inativado(AGORA);
        when(usuarios.buscarPorId(u.id())).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.alterar(new AlterarSenhaCommand("senhaAtual1", "novaSenha123"), solicitante(u)))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(usuarios, never()).salvar(any());
    }

    @Test
    @DisplayName("senha atual incorreta: recusado, nada gravado, sessões mantidas")
    void senhaAtualIncorreta() {
        Usuario u = usuario();
        when(usuarios.buscarPorId(u.id())).thenReturn(Optional.of(u));
        when(encoder.matches("errada123", "hash-atual")).thenReturn(false);

        assertThatThrownBy(() -> service.alterar(new AlterarSenhaCommand("errada123", "novaSenha123"), solicitante(u)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessage("Senha atual incorreta");
        verify(usuarios, never()).salvar(any());
        verifyNoInteractions(sessoes);
    }

    @Test
    @DisplayName("nova senha igual à atual é recusada")
    void igualAAtual() {
        Usuario u = usuario();
        when(usuarios.buscarPorId(u.id())).thenReturn(Optional.of(u));
        when(encoder.matches("senhaAtual1", "hash-atual")).thenReturn(true);

        assertThatThrownBy(() -> service.alterar(new AlterarSenhaCommand("senhaAtual1", "senhaAtual1"), solicitante(u)))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(usuarios, never()).salvar(any());
    }

    @Test
    @DisplayName("falha ao gravar vira OperacaoUsuarioException")
    void falhaAoGravar() {
        Usuario u = usuario();
        when(usuarios.buscarPorId(u.id())).thenReturn(Optional.of(u));
        when(encoder.matches("senhaAtual1", "hash-atual")).thenReturn(true);
        when(encoder.matches("novaSenha123", "hash-atual")).thenReturn(false);
        when(encoder.encode("novaSenha123")).thenReturn("hash-novo");
        when(usuarios.salvar(any())).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.alterar(new AlterarSenhaCommand("senhaAtual1", "novaSenha123"), solicitante(u)))
                .isInstanceOf(OperacaoUsuarioException.class);
        verifyNoInteractions(sessoes);
    }
}
