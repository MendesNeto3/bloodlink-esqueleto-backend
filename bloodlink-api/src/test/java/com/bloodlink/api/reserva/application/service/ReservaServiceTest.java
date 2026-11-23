package com.bloodlink.api.reserva.application.service;

import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.api.reserva.application.port.out.CriarReservaComando;
import com.bloodlink.api.reserva.application.port.out.ReservaRepositoryPort;
import com.bloodlink.api.reserva.domain.Reserva;
import com.bloodlink.api.reserva.domain.ReservaComSituacao;
import com.bloodlink.api.reserva.domain.SituacaoReserva;
import com.bloodlink.api.reserva.domain.StatusReserva;
import com.bloodlink.api.reserva.exception.OperacaoReservaException;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    /** 17/11/2026 12:00 em São Paulo (UTC-3). */
    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);
    private static final UUID USUARIO = UUID.randomUUID();

    @Mock
    private ReservaRepositoryPort repositorio;

    private ReservaService service;

    @BeforeEach
    void preparar() {
        service = new ReservaService(repositorio, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private CriarReservaComando comando(String hospital, String tipo, Integer unidades, LocalDate validaAte) {
        return new CriarReservaComando(hospital, tipo, unidades, validaAte, USUARIO);
    }

    private Reserva reserva(LocalDate validaAte, SituacaoReserva situacaoDesejada) {
        Reserva r = Reserva.nova("Hospital Central", TipoSanguineo.A_POSITIVO, 2, validaAte, USUARIO, AGORA);
        return situacaoDesejada == SituacaoReserva.LIBERADA ? r.liberar(HOJE, AGORA) : r;
    }

    // ---------- criar ----------

    @Test
    @DisplayName("criar grava a reserva com hospital normalizado e devolve situação ATIVA")
    void criarComSucesso() {
        when(repositorio.salvar(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        ReservaComSituacao resultado = service.criar(
                comando("  Hospital   Santa Maria ", "o-", 3, HOJE.plusDays(10)));

        assertThat(resultado.situacao()).isEqualTo(SituacaoReserva.ATIVA);
        Reserva r = resultado.reserva();
        assertThat(r.hospital()).isEqualTo("Hospital Santa Maria");
        assertThat(r.tipoSanguineo()).isEqualTo(TipoSanguineo.O_NEGATIVO);
        assertThat(r.unidades()).isEqualTo(3);
        assertThat(r.criadoPor()).isEqualTo(USUARIO);
        assertThat(r.criadoEm()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("aceita validade hoje e validade no limite de 365 dias")
    void validadeNosLimites() {
        when(repositorio.salvar(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.criar(comando("Hospital A", "A+", 1, HOJE)).situacao())
                .isEqualTo(SituacaoReserva.ATIVA);
        assertThat(service.criar(comando("Hospital A", "A+", 500, HOJE.plusDays(365))).situacao())
                .isEqualTo(SituacaoReserva.ATIVA);
    }

    @Test
    @DisplayName("usa o fuso de São Paulo: 01:00 UTC do dia 18 ainda é dia 17 no Brasil")
    void usaFusoDeSaoPaulo() {
        Clock viradaUtc = Clock.fixed(Instant.parse("2026-11-18T01:00:00Z"), ZoneOffset.UTC);
        ReservaService noite = new ReservaService(repositorio, viradaUtc);
        when(repositorio.salvar(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        // dia 17 ainda é "hoje" para o hemocentro, então não é passado
        assertThat(noite.criar(comando("Hospital A", "A+", 1, HOJE)).situacao())
                .isEqualTo(SituacaoReserva.ATIVA);
    }

    @Test
    @DisplayName("rejeita comando nulo")
    void rejeitaComandoNulo() {
        assertThatThrownBy(() -> service.criar(null)).isInstanceOf(IllegalArgumentException.class);
        verify(repositorio, never()).salvar(any());
    }

    @Test
    @DisplayName("rejeita hospital vazio, curto ou longo demais")
    void rejeitaHospitalInvalido() {
        for (String hospital : new String[]{null, "", "   ", "ab", "x".repeat(151)}) {
            assertThatThrownBy(() -> service.criar(comando(hospital, "A+", 1, HOJE.plusDays(1))))
                    .as("hospital: %s", hospital)
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verify(repositorio, never()).salvar(any());
    }

    @Test
    @DisplayName("rejeita tipo sanguíneo inválido")
    void rejeitaTipoInvalido() {
        for (String tipo : new String[]{null, "", "XYZ", "C+"}) {
            assertThatThrownBy(() -> service.criar(comando("Hospital A", tipo, 1, HOJE.plusDays(1))))
                    .as("tipo: %s", tipo)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("rejeita unidades fora de 1 a 500")
    void rejeitaUnidadesInvalidas() {
        for (Integer unidades : new Integer[]{null, 0, -1, 501}) {
            assertThatThrownBy(() -> service.criar(comando("Hospital A", "A+", unidades, HOJE.plusDays(1))))
                    .as("unidades: %s", unidades)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("rejeita validade nula, no passado ou acima de 365 dias")
    void rejeitaValidadeInvalida() {
        for (LocalDate validade : new LocalDate[]{null, HOJE.minusDays(1), HOJE.plusDays(366)}) {
            assertThatThrownBy(() -> service.criar(comando("Hospital A", "A+", 1, validade)))
                    .as("validade: %s", validade)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("rejeita quando não há usuário identificado")
    void rejeitaSemUsuario() {
        assertThatThrownBy(() -> service.criar(
                new CriarReservaComando("Hospital A", "A+", 1, HOJE.plusDays(1), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("falha do banco ao criar vira OperacaoReservaException sem vazar a causa na mensagem")
    void falhaDoBancoAoCriar() {
        when(repositorio.salvar(any(Reserva.class))).thenThrow(new RuntimeException("jdbc: senha=segredo"));

        assertThatThrownBy(() -> service.criar(comando("Hospital A", "A+", 1, HOJE.plusDays(1))))
                .isInstanceOf(OperacaoReservaException.class)
                .hasMessage("Não foi possível registrar a reserva no momento");
    }

    // ---------- listar ----------

    @Test
    @DisplayName("listar sem filtro devolve todas com a situação calculada")
    void listarSemFiltro() {
        when(repositorio.listarRecentes(anyInt())).thenReturn(List.of(
                reserva(HOJE.plusDays(3), SituacaoReserva.ATIVA),
                reserva(HOJE.minusDays(2), SituacaoReserva.ATIVA),
                reserva(HOJE.plusDays(3), SituacaoReserva.LIBERADA)));

        List<ReservaComSituacao> lista = service.listar(null, 50);

        assertThat(lista).extracting(ReservaComSituacao::situacao)
                .containsExactly(SituacaoReserva.ATIVA, SituacaoReserva.VENCIDA, SituacaoReserva.LIBERADA);
    }

    @Test
    @DisplayName("listar filtra por situação, inclusive VENCIDA (derivada da data)")
    void listarFiltraPorSituacao() {
        when(repositorio.listarRecentes(anyInt())).thenReturn(List.of(
                reserva(HOJE.plusDays(3), SituacaoReserva.ATIVA),
                reserva(HOJE.minusDays(2), SituacaoReserva.ATIVA),
                reserva(HOJE.plusDays(3), SituacaoReserva.LIBERADA)));

        assertThat(service.listar(SituacaoReserva.VENCIDA, 50)).hasSize(1);
        assertThat(service.listar(SituacaoReserva.LIBERADA, 50)).hasSize(1);
        assertThat(service.listar(SituacaoReserva.ATIVA, 50)).hasSize(1);
    }

    @Test
    @DisplayName("listar respeita o limite depois do filtro")
    void listarRespeitaLimite() {
        when(repositorio.listarRecentes(anyInt())).thenReturn(List.of(
                reserva(HOJE.plusDays(1), SituacaoReserva.ATIVA),
                reserva(HOJE.plusDays(2), SituacaoReserva.ATIVA),
                reserva(HOJE.plusDays(3), SituacaoReserva.ATIVA)));

        assertThat(service.listar(null, 2)).hasSize(2);
    }

    @Test
    @DisplayName("listar rejeita limite fora de 1 a 200")
    void listarRejeitaLimite() {
        assertThatThrownBy(() -> service.listar(null, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listar(null, 201)).isInstanceOf(IllegalArgumentException.class);
        verify(repositorio, never()).listarRecentes(anyInt());
    }

    @Test
    @DisplayName("falha do banco ao listar vira OperacaoReservaException")
    void falhaDoBancoAoListar() {
        when(repositorio.listarRecentes(anyInt())).thenThrow(new RuntimeException("falha"));

        assertThatThrownBy(() -> service.listar(null, 10)).isInstanceOf(OperacaoReservaException.class);
    }

    // ---------- liberar ----------

    @Test
    @DisplayName("liberar muda o status e grava")
    void liberarComSucesso() {
        Reserva ativa = reserva(HOJE.plusDays(5), SituacaoReserva.ATIVA);
        when(repositorio.buscarPorId(ativa.id())).thenReturn(Optional.of(ativa));
        when(repositorio.salvar(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        ReservaComSituacao resultado = service.liberar(ativa.id());

        assertThat(resultado.situacao()).isEqualTo(SituacaoReserva.LIBERADA);
        assertThat(resultado.reserva().status()).isEqualTo(StatusReserva.LIBERADA);
        assertThat(resultado.reserva().liberadaEm()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("liberar rejeita id nulo")
    void liberarIdNulo() {
        assertThatThrownBy(() -> service.liberar(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("liberar com id inexistente propaga 'não encontrado' sem embrulhar")
    void liberarNaoEncontrada() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.liberar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repositorio, never()).salvar(any());
    }

    @Test
    @DisplayName("liberar reserva já liberada ou vencida propaga conflito sem embrulhar")
    void liberarComConflito() {
        Reserva liberada = reserva(HOJE.plusDays(5), SituacaoReserva.LIBERADA);
        Reserva vencida = reserva(HOJE.minusDays(1), SituacaoReserva.ATIVA);
        when(repositorio.buscarPorId(liberada.id())).thenReturn(Optional.of(liberada));
        when(repositorio.buscarPorId(vencida.id())).thenReturn(Optional.of(vencida));

        assertThatThrownBy(() -> service.liberar(liberada.id())).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> service.liberar(vencida.id())).isInstanceOf(ConflitoException.class);
        verify(repositorio, never()).salvar(any());
    }

    @Test
    @DisplayName("falha do banco ao liberar vira OperacaoReservaException")
    void falhaDoBancoAoLiberar() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenThrow(new RuntimeException("falha"));

        assertThatThrownBy(() -> service.liberar(id)).isInstanceOf(OperacaoReservaException.class);
    }
}
