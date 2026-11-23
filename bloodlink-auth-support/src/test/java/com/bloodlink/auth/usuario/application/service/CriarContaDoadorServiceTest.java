package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.web.CriarContaDoadorCommand;
import com.bloodlink.auth.usuario.application.out.EnvioAtivacaoPort;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriarContaDoadorServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final String CENTRO = "95b82d103312";

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private TokenAtivacaoRepositoryPort tokens;
    @Mock
    private EnvioAtivacaoPort envio;

    private CriarContaDoadorService service;

    @BeforeEach
    void preparar() {
        service = new CriarContaDoadorService(usuarios, tokens, envio,
                Clock.fixed(AGORA, ZoneOffset.UTC), Duration.ofHours(48));
    }

    private CriarContaDoadorCommand comando(UUID doadorId) {
        return new CriarContaDoadorCommand("Maria Souza", "Maria@Exemplo.com", CENTRO, doadorId);
    }

    @Test
    @DisplayName("e-mail novo: cria conta pendente, gera token de 48 h e envia o link")
    void contaNova() {
        UUID doador = UUID.randomUUID();
        when(usuarios.buscarPorEmail("maria@exemplo.com")).thenReturn(Optional.empty());
        when(usuarios.salvar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UUID usuarioId = service.criar(comando(doador));

        ArgumentCaptor<Usuario> conta = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<TokenAtivacao> token = ArgumentCaptor.forClass(TokenAtivacao.class);
        verify(usuarios).salvar(conta.capture());
        verify(tokens).salvar(token.capture());
        verify(envio).enviar(any(), any(), any(), any());
        assertThat(conta.getValue().id()).isEqualTo(usuarioId);
        assertThat(conta.getValue().pendente()).isTrue();
        assertThat(conta.getValue().papel()).isEqualTo(Papel.DOADOR);
        assertThat(conta.getValue().doadorId()).isEqualTo(doador);
        assertThat(token.getValue().expiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(48)));
    }

    @Test
    @DisplayName("mesma conta ainda pendente: reaproveita e reenvia o link, invalidando o anterior")
    void reenvio() {
        UUID doador = UUID.randomUUID();
        Usuario existente = Usuario.doadorPendente(UUID.randomUUID(), "maria@exemplo.com", "Maria Souza", CENTRO,
                doador, AGORA.minusSeconds(3600));
        when(usuarios.buscarPorEmail("maria@exemplo.com")).thenReturn(Optional.of(existente));

        UUID usuarioId = service.criar(comando(doador));

        assertThat(usuarioId).isEqualTo(existente.id());
        verify(usuarios, never()).salvar(any());
        verify(tokens).invalidarPendentesDoUsuario(existente.id(), AGORA);
        verify(envio).enviar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("e-mail de outra conta (equipe, conta ativa ou outro doador): conflito, nada enviado")
    void conflito() {
        UUID doador = UUID.randomUUID();
        Usuario equipe = new Usuario(UUID.randomUUID(), "maria@exemplo.com", "Maria", "hash",
                Papel.ADMIN_CENTRO, CENTRO, true, AGORA, AGORA);
        Usuario ativaDoDoador = Usuario.doadorPendente(UUID.randomUUID(), "maria@exemplo.com", "Maria", CENTRO,
                doador, AGORA).ativadoComSenha("hash", AGORA);
        Usuario outroDoador = Usuario.doadorPendente(UUID.randomUUID(), "maria@exemplo.com", "Maria", CENTRO,
                UUID.randomUUID(), AGORA);
        Usuario outroCentro = Usuario.doadorPendente(UUID.randomUUID(), "maria@exemplo.com", "Maria",
                "aaaaaaaaaaaa", doador, AGORA);

        for (Usuario existente : new Usuario[]{equipe, ativaDoDoador, outroDoador, outroCentro}) {
            when(usuarios.buscarPorEmail("maria@exemplo.com")).thenReturn(Optional.of(existente));
            assertThatThrownBy(() -> service.criar(comando(doador))).isInstanceOf(ConflitoException.class);
        }
        verifyNoInteractions(envio);
        verify(tokens, never()).salvar(any());
    }

    @Test
    @DisplayName("dados inválidos são recusados antes de consultar o banco")
    void validacoes() {
        UUID doador = UUID.randomUUID();
        CriarContaDoadorCommand[] invalidos = {
                null,
                new CriarContaDoadorCommand("Al", "m@x.com", CENTRO, doador),
                new CriarContaDoadorCommand("Maria", "invalido", CENTRO, doador),
                new CriarContaDoadorCommand("Maria", "m@x.com", null, doador),
                new CriarContaDoadorCommand("Maria", "m@x.com", "95b82d10-3312-4a5b-8c9d-0123456789ab", doador),
                new CriarContaDoadorCommand("Maria", "m@x.com", CENTRO, null)
        };
        for (CriarContaDoadorCommand c : invalidos) {
            assertThatThrownBy(() -> service.criar(c)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(usuarios, tokens, envio);
    }

    @Test
    @DisplayName("falha inesperada vira OperacaoUsuarioException")
    void falha() {
        when(usuarios.buscarPorEmail(any())).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.criar(comando(UUID.randomUUID())))
                .isInstanceOf(OperacaoUsuarioException.class);
    }
}
