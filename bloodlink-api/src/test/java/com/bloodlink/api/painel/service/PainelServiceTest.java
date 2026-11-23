package com.bloodlink.api.painel.service;

import com.bloodlink.api.agendamento.domain.enums.StatusAgendamento;
import com.bloodlink.api.doador.adapter.out.persistence.DoacaoRepositoryPort;
import com.bloodlink.api.doador.adapter.out.persistence.ImpedimentoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.service.AvaliadorElegibilidade;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.painel.PainelRepositoryPort;
import com.bloodlink.api.painel.domain.PainelResumo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PainelServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);
    private static final List<StatusAgendamento> ATIVOS =
            List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO);

    @Mock
    private PainelRepositoryPort painel;
    @Mock
    private DoadorRepositoryPort doadores;
    @Mock
    private DoacaoRepositoryPort doacoes;
    @Mock
    private ImpedimentoRepositoryPort impedimentos;
    @Mock
    private AvaliadorElegibilidade avaliador;

    private PainelService service;

    @BeforeEach
    void preparar() {
        service = new PainelService(painel, doadores, doacoes, impedimentos, avaliador,
                Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZoneOffset.UTC));
        lenient().when(painel.contarDoacoes(any(), any())).thenReturn(0L);
        lenient().when(painel.contarDoadoresComDoacaoDesde(any())).thenReturn(0L);
        lenient().when(painel.contarDoadoresRecorrentesDesde(any())).thenReturn(0L);
        lenient().when(painel.contarAgendamentos(any(), any(), any())).thenReturn(0L);
        lenient().when(painel.diasComColeta(any(), any())).thenReturn(List.of());
        lenient().when(painel.proximasColetas(any(), anyInt())).thenReturn(List.of());
        lenient().when(painel.idsDoadoresRecentes(anyInt())).thenReturn(List.of());
    }

    @Test
    @DisplayName("ativosHoje conta agendamentos PENDENTE e CONFIRMADO apenas de hoje")
    void ativosHoje() {
        when(painel.contarAgendamentos(HOJE, HOJE, ATIVOS)).thenReturn(7L);

        PainelResumo r = service.obter(null, 5);

        assertThat(r.agendamentos().ativosHoje()).isEqualTo(7);
    }

    @Test
    @DisplayName("agendamentos: próximos 7 dias, confirmados de hoje e pendentes são contados separadamente")
    void contagensDeAgendamentos() {
        when(painel.contarAgendamentos(HOJE, HOJE.plusDays(6), ATIVOS)).thenReturn(4L);
        when(painel.contarAgendamentos(HOJE, HOJE, List.of(StatusAgendamento.CONFIRMADO))).thenReturn(1L);
        when(painel.contarAgendamentos(HOJE, HOJE.plusYears(1), List.of(StatusAgendamento.PENDENTE))).thenReturn(2L);

        PainelResumo.Agendamentos a = service.obter(null, 5).agendamentos();

        assertThat(a.proximos7Dias()).isEqualTo(4);
        assertThat(a.confirmadosHoje()).isEqualTo(1);
        assertThat(a.pendentes()).isEqualTo(2);
    }

    @Test
    @DisplayName("doações: hoje, últimos 7 dias, 7 anteriores e variação percentual com uma casa")
    void doacoesEVariacao() {
        when(painel.contarDoacoes(HOJE, HOJE)).thenReturn(3L);
        when(painel.contarDoacoes(HOJE.minusDays(6), HOJE)).thenReturn(15L);
        when(painel.contarDoacoes(HOJE.minusDays(13), HOJE.minusDays(7))).thenReturn(12L);

        PainelResumo.Doacoes d = service.obter(null, 5).doacoes();

        assertThat(d.hoje()).isEqualTo(3);
        assertThat(d.ultimos7Dias()).isEqualTo(15);
        assertThat(d.anteriores7Dias()).isEqualTo(12);
        assertThat(d.variacaoPercentual()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("variação é nula quando a semana anterior não teve doações; negativa quando caiu")
    void variacaoNulaOuNegativa() {
        assertThat(service.obter(null, 5).doacoes().variacaoPercentual()).isNull();

        when(painel.contarDoacoes(HOJE.minusDays(6), HOJE)).thenReturn(8L);
        when(painel.contarDoacoes(HOJE.minusDays(13), HOJE.minusDays(7))).thenReturn(10L);
        assertThat(service.obter(null, 5).doacoes().variacaoPercentual()).isEqualTo(-20.0);
    }

    @Test
    @DisplayName("mês escolhido define a contagem mensal e o calendário; sem mês, usa o mês de hoje")
    void mesEscolhido() {
        YearMonth outubro = YearMonth.of(2026, 10);
        when(painel.contarDoacoes(outubro.atDay(1), outubro.atEndOfMonth())).thenReturn(42L);
        when(painel.diasComColeta(outubro.atDay(1), outubro.atEndOfMonth())).thenReturn(List.of(3, 10));

        PainelResumo r = service.obter(outubro, 5);

        assertThat(r.doacoes().mes()).isEqualTo(42);
        assertThat(r.coletas().mes()).isEqualTo(outubro);
        assertThat(r.coletas().diasComColeta()).containsExactly(3, 10);
        assertThat(service.obter(null, 5).coletas().mes()).isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    @DisplayName("o serviço monta o painel sem nome do centro; comCentro preenche e mantém o resto")
    void comCentro() {
        PainelResumo semNome = service.obter(null, 5);

        PainelResumo comNome = semNome.comCentro("Hemocentro Central");

        assertThat(semNome.centroNome()).isNull();
        assertThat(comNome.centroNome()).isEqualTo("Hemocentro Central");
        assertThat(comNome.hoje()).isEqualTo(HOJE);
        assertThat(comNome.agendamentos()).isEqualTo(semNome.agendamentos());
        assertThat(comNome.coletas()).isEqualTo(semNome.coletas());
    }

    @Test
    @DisplayName("sem doadores recentes, não consulta doadores, doações nem impedimentos")
    void semRecentes() {
        PainelResumo r = service.obter(null, 5);

        assertThat(r.doadoresRecentes()).isEmpty();
    }

    @Test
    @DisplayName("limite de recentes é ajustado para o intervalo de 1 a 20")
    void limiteDeRecentes() {
        service.obter(null, 0);
        service.obter(null, 500);

        org.mockito.Mockito.verify(painel).idsDoadoresRecentes(1);
        org.mockito.Mockito.verify(painel).idsDoadoresRecentes(20);
    }

    @Test
    @DisplayName("falha em qualquer consulta vira OperacaoDoadorException")
    void falha() {
        when(painel.contarDoacoes(any(), any())).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.obter(null, 5)).isInstanceOf(OperacaoDoadorException.class);
    }
}
