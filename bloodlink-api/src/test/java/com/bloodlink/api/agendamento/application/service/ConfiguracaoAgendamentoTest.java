package com.bloodlink.api.agendamento.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguracaoAgendamentoTest {

    /** Terça-feira, 17/11/2026. */
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);
    private static final String TODOS_OS_DIAS = "SEG,TER,QUA,QUI,SEX,SAB,DOM";

    private ConfiguracaoAgendamento padrao() {
        return new ConfiguracaoAgendamento("08:00,09:00,10:00,11:00,13:00,14:00,15:00,16:00", 3, 1, 60,
                "SEG,TER,QUA,QUI,SEX", RELOGIO);
    }

    @Test
    @DisplayName("horários: ignora espaços e repetições e devolve ordenado")
    void horarios() {
        ConfiguracaoAgendamento c = new ConfiguracaoAgendamento(" 10:00, 08:30 ,10:00,,09:00", 2, 1, 30,
                "SEG", RELOGIO);

        assertThat(c.horarios()).containsExactly(LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertThat(c.vagasPorHorario()).isEqualTo(2);
    }

    @Test
    @DisplayName("dias de atendimento: segunda a sexta atendem; sábado e domingo não")
    void diasDeAtendimento() {
        ConfiguracaoAgendamento c = padrao();

        assertThat(c.atende(LocalDate.of(2026, 11, 18))).isTrue();    // quarta
        assertThat(c.atende(LocalDate.of(2026, 11, 20))).isTrue();    // sexta
        assertThat(c.atende(LocalDate.of(2026, 11, 21))).isFalse();   // sábado
        assertThat(c.atende(LocalDate.of(2026, 11, 22))).isFalse();   // domingo
    }

    @Test
    @DisplayName("dias aceitam minúsculas")
    void diasEmMinusculas() {
        ConfiguracaoAgendamento c = new ConfiguracaoAgendamento("08:00", 1, 0, 10, "sab,dom", RELOGIO);

        assertThat(c.atende(LocalDate.of(2026, 11, 21))).isTrue();
        assertThat(c.atende(LocalDate.of(2026, 11, 18))).isFalse();
    }

    @Test
    @DisplayName("doador: de amanhã até 60 dias; hoje e depois da janela são recusados")
    void janelaDoDoador() {
        ConfiguracaoAgendamento c = new ConfiguracaoAgendamento("08:00", 3, 1, 60, TODOS_OS_DIAS, RELOGIO);

        c.validarData(HOJE.plusDays(1));
        c.validarData(HOJE.plusDays(60));
        assertThatThrownBy(() -> c.validarData(HOJE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.validarData(HOJE.minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.validarData(HOJE.plusDays(61))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("mensagem da janela mostra as datas no formato brasileiro")
    void mensagemComDatas() {
        ConfiguracaoAgendamento c = new ConfiguracaoAgendamento("08:00", 3, 1, 60, TODOS_OS_DIAS, RELOGIO);

        assertThatThrownBy(() -> c.validarData(HOJE))
                .hasMessageContaining("18/11/2026").hasMessageContaining("16/01/2027");
    }

    @Test
    @DisplayName("equipe: de hoje até 60 dias, sem a antecedência mínima")
    void janelaDaEquipe() {
        ConfiguracaoAgendamento c = new ConfiguracaoAgendamento("08:00", 3, 1, 60, TODOS_OS_DIAS, RELOGIO);

        c.validarDataParaEquipe(HOJE);
        c.validarDataParaEquipe(HOJE.plusDays(60));
        assertThatThrownBy(() -> c.validarDataParaEquipe(HOJE.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.validarDataParaEquipe(HOJE.plusDays(61)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("configuração inválida impede a aplicação de subir")
    void configuracaoInvalida() {
        assertThatThrownBy(() -> new ConfiguracaoAgendamento("", 3, 1, 60, "SEG", RELOGIO))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ConfiguracaoAgendamento("08:00", 3, 1, 60, "", RELOGIO))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ConfiguracaoAgendamento("08:00", 3, 1, 60, "XYZ", RELOGIO))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ConfiguracaoAgendamento("08:00", 0, 1, 60, "SEG", RELOGIO))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ConfiguracaoAgendamento("08:00", 3, -1, 60, "SEG", RELOGIO))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ConfiguracaoAgendamento("08:00", 3, 10, 5, "SEG", RELOGIO))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ConfiguracaoAgendamento("8h", 3, 1, 60, "SEG", RELOGIO))
                .isInstanceOf(RuntimeException.class);
    }
}
