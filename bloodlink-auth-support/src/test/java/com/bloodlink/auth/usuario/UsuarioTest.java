package com.bloodlink.auth.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private static final Instant T0 = Instant.parse("2026-11-17T15:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);

    private Usuario equipe() {
        return new Usuario(UUID.randomUUID(), "ana@centro.com", "Ana Souza", "hash-secreto",
                Papel.ADMIN_CENTRO, "95b82d103312", true, T0, T0);
    }

    @Test
    @DisplayName("construtor da equipe nasce ativo, sem doador")
    void equipeNasceAtiva() {
        Usuario u = equipe();

        assertThat(u.status()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(u.doadorId()).isNull();
        assertThat(u.podeLogar()).isTrue();
    }

    @Test
    @DisplayName("conta de doador nasce pendente, sem senha, e não loga")
    void doadorPendente() {
        UUID doador = UUID.randomUUID();
        Usuario u = Usuario.doadorPendente(UUID.randomUUID(), "m@x.com", "Maria", "95b82d103312", doador, T0);

        assertThat(u.pendente()).isTrue();
        assertThat(u.papel()).isEqualTo(Papel.DOADOR);
        assertThat(u.senhaHash()).isNull();
        assertThat(u.doadorId()).isEqualTo(doador);
        assertThat(u.podeLogar()).isFalse();
    }

    @Test
    @DisplayName("ativar com senha torna a conta ativa e apta a logar")
    void ativar() {
        Usuario pendente = Usuario.doadorPendente(UUID.randomUUID(), "m@x.com", "Maria", "95b82d103312",
                UUID.randomUUID(), T0);

        Usuario ativa = pendente.ativadoComSenha("novo-hash", T1);

        assertThat(ativa.pendente()).isFalse();
        assertThat(ativa.senhaHash()).isEqualTo("novo-hash");
        assertThat(ativa.podeLogar()).isTrue();
        assertThat(ativa.atualizadoEm()).isEqualTo(T1);
    }

    @Test
    @DisplayName("inativado não loga; reativado volta a logar")
    void inativarReativar() {
        Usuario u = equipe();

        assertThat(u.inativado(T1).podeLogar()).isFalse();
        assertThat(u.inativado(T1).reativado(T1).podeLogar()).isTrue();
    }

    @Test
    @DisplayName("comSenha troca o hash e mantém o status")
    void comSenha() {
        Usuario u = equipe().comSenha("outro-hash", T1);

        assertThat(u.senhaHash()).isEqualTo("outro-hash");
        assertThat(u.status()).isEqualTo(StatusUsuario.ATIVO);
    }

    @Test
    @DisplayName("toString não expõe hash da senha, e-mail nem nome")
    void toStringSeguro() {
        String texto = equipe().toString();

        assertThat(texto).doesNotContain("hash-secreto").doesNotContain("ana@centro.com").doesNotContain("Ana Souza");
    }
}
