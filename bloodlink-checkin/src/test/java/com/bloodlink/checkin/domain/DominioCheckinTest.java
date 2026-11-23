package com.bloodlink.checkin.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DominioCheckinTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");

    @Test
    @DisplayName("situação: concluído e faltou valem mais que qualquer outra coisa")
    void situacaoTerminais() {
        assertThat(SituacaoCheckin.de("CONCLUIDO", true, ResultadoTriagem.APTA)).isEqualTo(SituacaoCheckin.CONCLUIDO);
        assertThat(SituacaoCheckin.de("CONCLUIDO", true, ResultadoTriagem.INAPTA)).isEqualTo(SituacaoCheckin.CONCLUIDO);
        assertThat(SituacaoCheckin.de("FALTOU", false, null)).isEqualTo(SituacaoCheckin.FALTOU);
    }

    @Test
    @DisplayName("situação: inapto vale mais que cancelado (o cancelamento foi causado pela triagem)")
    void situacaoInapto() {
        assertThat(SituacaoCheckin.de("CANCELADO", true, ResultadoTriagem.INAPTA)).isEqualTo(SituacaoCheckin.INAPTO);
        assertThat(SituacaoCheckin.de("CONFIRMADO", true, ResultadoTriagem.INAPTA)).isEqualTo(SituacaoCheckin.INAPTO);
        assertThat(SituacaoCheckin.de("CANCELADO", true, null)).isEqualTo(SituacaoCheckin.CANCELADO);
        assertThat(SituacaoCheckin.de("CANCELADO", false, null)).isEqualTo(SituacaoCheckin.CANCELADO);
    }

    @Test
    @DisplayName("situação: sem check-in não chegou; com check-in aguarda triagem ou está apto")
    void situacaoEmAndamento() {
        assertThat(SituacaoCheckin.de("CONFIRMADO", false, null)).isEqualTo(SituacaoCheckin.NAO_CHEGOU);
        assertThat(SituacaoCheckin.de("PENDENTE", false, null)).isEqualTo(SituacaoCheckin.NAO_CHEGOU);
        assertThat(SituacaoCheckin.de("CONFIRMADO", true, null)).isEqualTo(SituacaoCheckin.AGUARDANDO_TRIAGEM);
        assertThat(SituacaoCheckin.de("CONFIRMADO", true, ResultadoTriagem.APTA)).isEqualTo(SituacaoCheckin.APTO);
    }

    @Test
    @DisplayName("filtro de situação aceita minúsculas e espaços; inválido lista as opções")
    void filtro() {
        assertThat(SituacaoCheckin.doFiltro(" apto ")).isEqualTo(SituacaoCheckin.APTO);
        assertThat(SituacaoCheckin.doFiltro("NAO_CHEGOU")).isEqualTo(SituacaoCheckin.NAO_CHEGOU);
        assertThatThrownBy(() -> SituacaoCheckin.doFiltro("xyz"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("NAO_CHEGOU");
    }

    @Test
    @DisplayName("resultado da triagem: APTA ou INAPTA, sem diferenciar maiúsculas")
    void resultado() {
        assertThat(ResultadoTriagem.de("apta")).isEqualTo(ResultadoTriagem.APTA);
        assertThat(ResultadoTriagem.de(" INAPTA ")).isEqualTo(ResultadoTriagem.INAPTA);
        for (String ruim : new String[]{null, "", "  ", "talvez"}) {
            assertThatThrownBy(() -> ResultadoTriagem.de(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("check-in novo nasce sem triagem; comTriagem preenche sem alterar o original")
    void checkin() {
        UUID ag = UUID.randomUUID();
        UUID doador = UUID.randomUUID();
        UUID recepcao = UUID.randomUUID();
        UUID enfermeiro = UUID.randomUUID();

        Checkin novo = Checkin.novo(ag, doador, recepcao, AGORA);
        Checkin triado = novo.comTriagem(ResultadoTriagem.INAPTA, "pressão alta", enfermeiro, AGORA.plusSeconds(60));

        assertThat(novo.triado()).isFalse();
        assertThat(novo.triagem()).isNull();
        assertThat(novo.id()).isNotNull();
        assertThat(triado.triado()).isTrue();
        assertThat(triado.id()).isEqualTo(novo.id());
        assertThat(triado.chegadaEm()).isEqualTo(AGORA);
        assertThat(triado.triagemPor()).isEqualTo(enfermeiro);
        assertThat(triado.triagemEm()).isEqualTo(AGORA.plusSeconds(60));
    }

    @Test
    @DisplayName("toString do check-in e do item da fila não vaza dado de saúde nem nome")
    void toStringSeguro() {
        Checkin c = Checkin.novo(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AGORA)
                .comTriagem(ResultadoTriagem.INAPTA, "HIV positivo", UUID.randomUUID(), AGORA);
        ItemFila item = new ItemFila(UUID.randomUUID(), c.id(), c.doadorId(), "Maria Souza", "O_POSITIVO",
                java.time.LocalTime.of(9, 0), "CONFIRMADO", SituacaoCheckin.INAPTO, AGORA,
                ResultadoTriagem.INAPTA, "HIV positivo", AGORA);

        assertThat(c.toString()).doesNotContain("HIV");
        assertThat(item.toString()).doesNotContain("HIV").doesNotContain("Maria");
    }
}
