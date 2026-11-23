package com.bloodlink.checkin.application.service;

import com.bloodlink.checkin.application.exception.ConflitoException;
import com.bloodlink.checkin.application.exception.OperacaoCheckinException;
import com.bloodlink.checkin.application.exception.RecursoNaoEncontradoException;
import com.bloodlink.checkin.application.exception.RegraDeNegocioException;
import com.bloodlink.checkin.application.port.out.CheckinRepositoryPort;
import com.bloodlink.checkin.domain.AgendamentoParaCheckin;
import com.bloodlink.checkin.domain.Checkin;
import com.bloodlink.checkin.domain.DetalheCheckin;
import com.bloodlink.checkin.domain.ItemFila;
import com.bloodlink.checkin.domain.ResultadoTriagem;
import com.bloodlink.checkin.domain.ResumoFila;
import com.bloodlink.checkin.domain.SituacaoCheckin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
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
class CheckinServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);
    private static final UUID AGENDAMENTO = UUID.randomUUID();
    private static final UUID DOADOR = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    @Mock
    private CheckinRepositoryPort repo;

    private CheckinService service;

    @BeforeEach
    void preparar() {
        service = new CheckinService(repo, Clock.fixed(AGORA, ZoneId.of("America/Sao_Paulo")));
    }

    private AgendamentoParaCheckin agendamento(String status, LocalDate data) {
        return new AgendamentoParaCheckin(AGENDAMENTO, DOADOR, data, status);
    }

    private Checkin checkin() {
        return Checkin.novo(AGENDAMENTO, DOADOR, USUARIO, AGORA);
    }

    // ---------- registrarChegada ----------

    @Test
    @DisplayName("chegada: agendamento confirmado de hoje gera check-in aguardando triagem")
    void chegada() {
        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.of(agendamento("CONFIRMADO", HOJE)));
        when(repo.inserir(any())).thenReturn(true);

        DetalheCheckin d = service.registrarChegada(AGENDAMENTO, USUARIO);

        ArgumentCaptor<Checkin> salvo = ArgumentCaptor.forClass(Checkin.class);
        verify(repo).inserir(salvo.capture());
        assertThat(salvo.getValue().agendamentoId()).isEqualTo(AGENDAMENTO);
        assertThat(salvo.getValue().doadorId()).isEqualTo(DOADOR);
        assertThat(salvo.getValue().registradoPor()).isEqualTo(USUARIO);
        assertThat(salvo.getValue().chegadaEm()).isEqualTo(AGORA);
        assertThat(d.situacao()).isEqualTo(SituacaoCheckin.AGUARDANDO_TRIAGEM);
    }

    @Test
    @DisplayName("chegada: ids obrigatórios, validados antes de consultar o banco")
    void chegadaIds() {
        assertThatThrownBy(() -> service.registrarChegada(null, USUARIO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarChegada(AGENDAMENTO, null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("chegada: agendamento inexistente, não confirmado ou de outro dia")
    void chegadaRecusas() {
        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registrarChegada(AGENDAMENTO, USUARIO))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.of(agendamento("PENDENTE", HOJE)));
        assertThatThrownBy(() -> service.registrarChegada(AGENDAMENTO, USUARIO))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("confirmado");

        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.of(agendamento("CONFIRMADO", HOJE.plusDays(1))));
        assertThatThrownBy(() -> service.registrarChegada(AGENDAMENTO, USUARIO))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("dia do agendamento");

        verify(repo, never()).inserir(any());
    }

    @Test
    @DisplayName("chegada duplicada (o banco recusa o segundo insert): conflito")
    void chegadaDuplicada() {
        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.of(agendamento("CONFIRMADO", HOJE)));
        when(repo.inserir(any())).thenReturn(false);

        assertThatThrownBy(() -> service.registrarChegada(AGENDAMENTO, USUARIO)).isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("chegada: falha do banco vira OperacaoCheckinException sem expor a causa")
    void chegadaFalhaDoBanco() {
        when(repo.buscarAgendamento(AGENDAMENTO)).thenThrow(new RuntimeException("jdbc: senha=123"));

        assertThatThrownBy(() -> service.registrarChegada(AGENDAMENTO, USUARIO))
                .isInstanceOf(OperacaoCheckinException.class)
                .hasMessage("Não foi possível registrar o check-in no momento");
    }

    // ---------- registrarTriagem ----------

    @Test
    @DisplayName("triagem APTA grava, sem exigir observação")
    void triagemApta() {
        Checkin atual = checkin();
        when(repo.buscarPorIdParaAtualizar(atual.id())).thenReturn(Optional.of(atual));
        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.of(agendamento("CONFIRMADO", HOJE)));
        when(repo.atualizarTriagem(any())).thenReturn(true);

        DetalheCheckin d = service.registrarTriagem(atual.id(), "apta", "  ", USUARIO);

        assertThat(d.situacao()).isEqualTo(SituacaoCheckin.APTO);
        assertThat(d.checkin().triagem()).isEqualTo(ResultadoTriagem.APTA);
        assertThat(d.checkin().triagemObservacao()).isNull();
        assertThat(d.checkin().triagemPor()).isEqualTo(USUARIO);
        assertThat(d.checkin().triagemEm()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("triagem INAPTA exige o motivo e o guarda sem espaços")
    void triagemInapta() {
        Checkin atual = checkin();
        when(repo.buscarPorIdParaAtualizar(atual.id())).thenReturn(Optional.of(atual));
        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.of(agendamento("CONFIRMADO", HOJE)));
        when(repo.atualizarTriagem(any())).thenReturn(true);

        DetalheCheckin d = service.registrarTriagem(atual.id(), "INAPTA", "  pressão alta ", USUARIO);

        assertThat(d.situacao()).isEqualTo(SituacaoCheckin.INAPTO);
        assertThat(d.checkin().triagemObservacao()).isEqualTo("pressão alta");
    }

    @Test
    @DisplayName("triagem: entradas inválidas são recusadas antes do banco")
    void triagemValidacoes() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.registrarTriagem(null, "APTA", null, USUARIO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarTriagem(id, "APTA", null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarTriagem(id, null, null, USUARIO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarTriagem(id, "TALVEZ", null, USUARIO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarTriagem(id, "INAPTA", null, USUARIO))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("motivo");
        assertThatThrownBy(() -> service.registrarTriagem(id, "INAPTA", "   ", USUARIO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarTriagem(id, "APTA", "x".repeat(501), USUARIO))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("triagem: check-in inexistente, já triado, agendamento sumido, não confirmado ou de outro dia")
    void triagemRecusas() {
        UUID id = UUID.randomUUID();
        when(repo.buscarPorIdParaAtualizar(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registrarTriagem(id, "APTA", null, USUARIO))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        Checkin jaTriado = checkin().comTriagem(ResultadoTriagem.APTA, null, USUARIO, AGORA);
        when(repo.buscarPorIdParaAtualizar(jaTriado.id())).thenReturn(Optional.of(jaTriado));
        assertThatThrownBy(() -> service.registrarTriagem(jaTriado.id(), "APTA", null, USUARIO))
                .isInstanceOf(ConflitoException.class);

        Checkin atual = checkin();
        when(repo.buscarPorIdParaAtualizar(atual.id())).thenReturn(Optional.of(atual));
        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registrarTriagem(atual.id(), "APTA", null, USUARIO))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.of(agendamento("CANCELADO", HOJE)));
        assertThatThrownBy(() -> service.registrarTriagem(atual.id(), "APTA", null, USUARIO))
                .isInstanceOf(RegraDeNegocioException.class);

        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.of(agendamento("CONFIRMADO", HOJE.minusDays(1))));
        assertThatThrownBy(() -> service.registrarTriagem(atual.id(), "APTA", null, USUARIO))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(repo, never()).atualizarTriagem(any());
    }

    @Test
    @DisplayName("triagem registrada por outra requisição ao mesmo tempo: conflito")
    void triagemConcorrente() {
        Checkin atual = checkin();
        when(repo.buscarPorIdParaAtualizar(atual.id())).thenReturn(Optional.of(atual));
        when(repo.buscarAgendamento(AGENDAMENTO)).thenReturn(Optional.of(agendamento("CONFIRMADO", HOJE)));
        when(repo.atualizarTriagem(any())).thenReturn(false);

        assertThatThrownBy(() -> service.registrarTriagem(atual.id(), "APTA", null, USUARIO))
                .isInstanceOf(ConflitoException.class);
    }

    // ---------- buscar ----------

    @Test
    @DisplayName("buscar devolve o detalhe, falha para id nulo ou inexistente")
    void buscar() {
        DetalheCheckin detalhe = new DetalheCheckin(checkin(), SituacaoCheckin.AGUARDANDO_TRIAGEM);
        UUID inexistente = UUID.randomUUID();
        when(repo.buscarDetalhePorId(detalhe.checkin().id())).thenReturn(Optional.of(detalhe));
        when(repo.buscarDetalhePorId(inexistente)).thenReturn(Optional.empty());

        assertThat(service.buscar(detalhe.checkin().id())).isEqualTo(detalhe);
        assertThatThrownBy(() -> service.buscar(inexistente)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.buscar(null)).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- fila ----------

    private ItemFila item(SituacaoCheckin situacao) {
        return new ItemFila(UUID.randomUUID(), null, UUID.randomUUID(), "Maria", "O_POSITIVO", LocalTime.of(9, 0),
                "CONFIRMADO", situacao, null, null, null, null);
    }

    @Test
    @DisplayName("fila: sem data usa hoje, soma por situação (todas as chaves presentes) e devolve todos os itens")
    void fila() {
        when(repo.fila(HOJE)).thenReturn(List.of(item(SituacaoCheckin.NAO_CHEGOU), item(SituacaoCheckin.NAO_CHEGOU),
                item(SituacaoCheckin.APTO)));

        ResumoFila r = service.fila(null, null);

        assertThat(r.data()).isEqualTo(HOJE);
        assertThat(r.itens()).hasSize(3);
        assertThat(r.totais()).containsEntry(SituacaoCheckin.NAO_CHEGOU, 2L)
                .containsEntry(SituacaoCheckin.APTO, 1L)
                .containsEntry(SituacaoCheckin.INAPTO, 0L);
        assertThat(r.totais()).hasSize(SituacaoCheckin.values().length);
    }

    @Test
    @DisplayName("fila: filtro por situação reduz os itens mas os totais continuam do dia inteiro")
    void filaFiltrada() {
        LocalDate dia = LocalDate.of(2026, 12, 1);
        when(repo.fila(dia)).thenReturn(List.of(item(SituacaoCheckin.NAO_CHEGOU), item(SituacaoCheckin.APTO)));

        ResumoFila r = service.fila(dia, " apto ");

        assertThat(r.data()).isEqualTo(dia);
        assertThat(r.itens()).hasSize(1);
        assertThat(r.itens().get(0).situacao()).isEqualTo(SituacaoCheckin.APTO);
        assertThat(r.totais().get(SituacaoCheckin.NAO_CHEGOU)).isEqualTo(1L);
    }

    @Test
    @DisplayName("fila: situação inválida é recusada; filtro em branco vale como todas; falha do banco vira OperacaoCheckinException")
    void filaErros() {
        assertThatThrownBy(() -> service.fila(null, "xyz")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);

        when(repo.fila(HOJE)).thenReturn(List.of(item(SituacaoCheckin.APTO)));
        assertThat(service.fila(null, "  ").itens()).hasSize(1);

        when(repo.fila(HOJE)).thenThrow(new RuntimeException("banco fora"));
        assertThatThrownBy(() -> service.fila(null, null)).isInstanceOf(OperacaoCheckinException.class);
    }
}
