package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.out.EnvioRedefinicaoSenhaPort;
import com.bloodlink.auth.usuario.application.out.TokenAtivacaoRepositoryPort;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitarRedefinicaoSenhaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private TokenAtivacaoRepositoryPort tokens;
    @Mock
    private EnvioRedefinicaoSenhaPort envio;

    private SolicitarRedefinicaoSenhaService service;

    @BeforeEach
    void preparar() {
        service = new SolicitarRedefinicaoSenhaService(usuarios, tokens, envio,
                Clock.fixed(AGORA, ZoneOffset.UTC), Duration.ofHours(1), 3);
    }

    private Usuario ativo() {
        return new Usuario(UUID.randomUUID(), "ana@centro.com", "Ana Souza", "hash", Papel.ADMIN_CENTRO,
                "95b82d103312", true, AGORA, AGORA);
    }

    @Test
    @DisplayName("conta ativa: invalida links antigos, grava só o hash e envia o token por e-mail")
    void sucesso() {
        Usuario u = ativo();
        when(usuarios.buscarPorEmail("ana@centro.com")).thenReturn(Optional.of(u));
        when(tokens.contarCriadosDesde(u.id(), AGORA.minus(Duration.ofHours(1)))).thenReturn(0L);

        service.solicitar("  Ana@Centro.com ");

        ArgumentCaptor<TokenAtivacao> salvo = ArgumentCaptor.forClass(TokenAtivacao.class);
        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(tokens).invalidarPendentesDoUsuario(u.id(), AGORA);
        verify(tokens).salvar(salvo.capture());
        verify(envio).enviar(anyString(), anyString(), enviado.capture(), any(Instant.class));
        assertThat(salvo.getValue().usuarioId()).isEqualTo(u.id());
        assertThat(salvo.getValue().expiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(1)));
        assertThat(salvo.getValue().tokenHash()).isEqualTo(TokensOpacos.hash(enviado.getValue()));
        assertThat(salvo.getValue().tokenHash()).isNotEqualTo(enviado.getValue());
    }

    @Test
    @DisplayName("e-mail sem conta: não envia nada e não lança erro")
    void semConta() {
        when(usuarios.buscarPorEmail("ninguem@x.com")).thenReturn(Optional.empty());

        service.solicitar("ninguem@x.com");

        verifyNoInteractions(envio);
        verify(tokens, never()).salvar(any());
    }

    @Test
    @DisplayName("conta pendente de ativação ou inativa: não envia nada")
    void contaNaoApta() {
        Usuario pendente = Usuario.doadorPendente(UUID.randomUUID(), "m@x.com", "Maria", "95b82d103312",
                UUID.randomUUID(), AGORA);
        when(usuarios.buscarPorEmail("m@x.com")).thenReturn(Optional.of(pendente));
        service.solicitar("m@x.com");

        when(usuarios.buscarPorEmail("ana@centro.com")).thenReturn(Optional.of(ativo().inativado(AGORA)));
        service.solicitar("ana@centro.com");

        verifyNoInteractions(envio);
        verify(tokens, never()).salvar(any());
    }

    @Test
    @DisplayName("limite de 3 pedidos por hora: o quarto é ignorado em silêncio")
    void limitePorHora() {
        Usuario u = ativo();
        when(usuarios.buscarPorEmail("ana@centro.com")).thenReturn(Optional.of(u));
        when(tokens.contarCriadosDesde(u.id(), AGORA.minus(Duration.ofHours(1)))).thenReturn(3L);

        service.solicitar("ana@centro.com");

        verifyNoInteractions(envio);
        verify(tokens, never()).salvar(any());
    }

    @Test
    @DisplayName("e-mail em formato inválido é recusado antes de consultar o banco")
    void emailInvalido() {
        for (String ruim : new String[]{null, "", "  ", "invalido"}) {
            assertThatThrownBy(() -> service.solicitar(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(usuarios, tokens, envio);
    }

    @Test
    @DisplayName("falha no envio do e-mail é engolida, para não revelar quem tem conta")
    void falhaNoEnvio() {
        Usuario u = ativo();
        when(usuarios.buscarPorEmail("ana@centro.com")).thenReturn(Optional.of(u));
        doThrow(new RuntimeException("smtp fora")).when(envio).enviar(anyString(), anyString(), anyString(), any());

        service.solicitar("ana@centro.com");   // não lança

        verify(tokens).salvar(any());
    }

    @Test
    @DisplayName("falha do banco vira OperacaoUsuarioException")
    void falhaDoBanco() {
        when(usuarios.buscarPorEmail(anyString())).thenThrow(new RuntimeException("banco fora"));
        assertThatThrownBy(() -> service.solicitar("ana@centro.com")).isInstanceOf(OperacaoUsuarioException.class);

        Usuario u = ativo();
        doReturn(Optional.of(u)).when(usuarios).buscarPorEmail("outra@centro.com");
        when(tokens.salvar(any())).thenThrow(new RuntimeException("banco fora"));
        assertThatThrownBy(() -> service.solicitar("outra@centro.com")).isInstanceOf(OperacaoUsuarioException.class);
        verifyNoInteractions(envio);
    }
}
