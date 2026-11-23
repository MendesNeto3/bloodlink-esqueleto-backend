package com.bloodlink.api.agendamento.application.service;

import com.bloodlink.api.agendamento.adapter.in.web.dto.AgendamentoComDoador;
import com.bloodlink.api.agendamento.application.port.out.AgendamentoEventosPort;
import com.bloodlink.api.agendamento.application.port.out.AgendamentoRepositoryPort;
import com.bloodlink.api.agendamento.application.port.out.CentroConsultaPort;
import com.bloodlink.api.agendamento.domain.Agendamento;
import com.bloodlink.api.agendamento.domain.FiltroAgendamento;
import com.bloodlink.api.agendamento.domain.SolicitarAgendamentoComando;
import com.bloodlink.api.agendamento.domain.enums.OrigemAgendamento;
import com.bloodlink.api.agendamento.domain.enums.StatusAgendamento;
import com.bloodlink.api.doador.adapter.out.persistence.DoacaoRepositoryPort;
import com.bloodlink.api.doador.adapter.out.persistence.ImpedimentoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoacaoUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.out.RegistrarDoacaoComando;
import com.bloodlink.api.doador.application.port.out.ResultadoElegibilidade;
import com.bloodlink.api.doador.application.service.AvaliadorElegibilidade;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doacao;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.MotivoInelegibilidade;
import com.bloodlink.api.doador.domain.SexoBiologico;
import com.bloodlink.api.doador.domain.StatusElegibilidade;
import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    /** Terça-feira, 17/11/2026, 12:00 em São Paulo. */
    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);
    private static final LocalDate QUARTA = LocalDate.of(2026, 11, 18);
    private static final LocalDate SABADO = LocalDate.of(2026, 11, 21);
    private static final UUID DOADOR = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    @Mock
    private AgendamentoRepositoryPort agendamentos;
    @Mock
    private DoadorRepositoryPort doadores;
    @Mock
    private DoacaoRepositoryPort doacoes;
    @Mock
    private ImpedimentoRepositoryPort impedimentos;
    @Mock
    private DoacaoUseCase doacaoUseCase;
    @Mock
    private AvaliadorElegibilidade avaliador;
    @Mock
    private AgendamentoEventosPort eventos;
    @Mock
    private CentroConsultaPort centros;

    private AgendamentoService service;

    @BeforeEach
    void preparar() {
        Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
        ConfiguracaoAgendamento config = new ConfiguracaoAgendamento(
                "08:00,09:00,10:00,11:00,13:00,14:00,15:00,16:00", 3, 1, 60, "SEG,TER,QUA,QUI,SEX", clock);
        service = new AgendamentoService(agendamentos, doadores, doacoes, impedimentos, doacaoUseCase,
                avaliador, config, eventos, centros, clock);
        lenient().when(agendamentos.salvar(any(Agendamento.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Doador doador(boolean ativo, String email) {
        return new Doador(DOADOR, "Maria Souza", "52998224725", LocalDate.of(1990, 5, 10), SexoBiologico.FEMININO,
                TipoSanguineo.O_POSITIVO, new BigDecimal("62.5"), "11987654321", email, false, ativo, AGORA, AGORA);
    }

    private SolicitarAgendamentoComando comando(LocalDate data, LocalTime hora, OrigemAgendamento origem) {
        return new SolicitarAgendamentoComando(DOADOR, data, hora, "  primeira vez ", origem, USUARIO);
    }

    private void doadorElegivel() {
        when(doadores.buscarPorId(DOADOR)).thenReturn(Optional.of(doador(true, "m@x.com")));
        when(agendamentos.buscarAtivoDoDoador(DOADOR)).thenReturn(Optional.empty());
        when(doacoes.datasPorDoadores(any())).thenReturn(Map.of());
        when(impedimentos.naoEncerradosPorDoadores(any())).thenReturn(Map.of());
        when(avaliador.avaliarEm(any(), any(), any(), any()))
                .thenReturn(new ResultadoElegibilidade(StatusElegibilidade.ELEGIVEL, List.of(), null));
    }

    private Agendamento agendamento(StatusAgendamento status, LocalDate data) {
        Agendamento base = Agendamento.novo(DOADOR, data, LocalTime.of(9, 0), OrigemAgendamento.DOADOR, null,
                USUARIO, AGORA);
        return switch (status) {
            case PENDENTE -> base;
            case CONFIRMADO -> base.confirmar(AGORA);
            case CANCELADO -> base.cancelar("x", AGORA);
            case CONCLUIDO -> base.confirmar(AGORA).concluir(UUID.randomUUID(), AGORA);
            case FALTOU -> base.confirmar(AGORA).registrarFalta(AGORA);
        };
    }

    private Agendamento encontrado(StatusAgendamento status, LocalDate data) {
        Agendamento a = agendamento(status, data);
        when(agendamentos.buscarPorId(a.id())).thenReturn(Optional.of(a));
        return a;
    }

    // ---------- solicitar ----------

    @Test
    @DisplayName("doador elegível, horário livre: cria agendamento PENDENTE com observação limpa")
    void solicitarComSucesso() {
        doadorElegivel();
        when(agendamentos.contarAtivosNoHorario(QUARTA, LocalTime.of(9, 0))).thenReturn(2L);

        Agendamento a = service.solicitar(comando(QUARTA, LocalTime.of(9, 0), OrigemAgendamento.DOADOR));

        assertThat(a.status()).isEqualTo(StatusAgendamento.PENDENTE);
        assertThat(a.doadorId()).isEqualTo(DOADOR);
        assertThat(a.date()).isEqualTo(QUARTA);
        assertThat(a.observacao()).isEqualTo("primeira vez");
        assertThat(a.origem()).isEqualTo(OrigemAgendamento.DOADOR);
        assertThat(a.criadoEm()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("a elegibilidade é avaliada para o dia escolhido, não para hoje")
    void elegibilidadeNoDiaEscolhido() {
        doadorElegivel();

        service.solicitar(comando(QUARTA, LocalTime.of(9, 0), OrigemAgendamento.DOADOR));

        verify(avaliador).avaliarEm(any(), any(), any(), eq(QUARTA));
    }

    @Test
    @DisplayName("segundos do horário são descartados")
    void horaTruncada() {
        doadorElegivel();

        Agendamento a = service.solicitar(comando(QUARTA, LocalTime.of(9, 0, 45), OrigemAgendamento.DOADOR));

        assertThat(a.hora()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("a equipe pode encaixar para hoje; o doador não")
    void hojeSoParaEquipe() {
        doadorElegivel();

        assertThat(service.solicitar(comando(HOJE, LocalTime.of(14, 0), OrigemAgendamento.EQUIPE)).date())
                .isEqualTo(HOJE);
        assertThatThrownBy(() -> service.solicitar(comando(HOJE, LocalTime.of(14, 0), OrigemAgendamento.DOADOR)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("dados inválidos são recusados antes de consultar o banco")
    void validacoes() {
        SolicitarAgendamentoComando[] invalidos = {
                null,
                new SolicitarAgendamentoComando(null, QUARTA, LocalTime.of(9, 0), null, OrigemAgendamento.DOADOR, USUARIO),
                new SolicitarAgendamentoComando(DOADOR, QUARTA, LocalTime.of(9, 0), null, OrigemAgendamento.DOADOR, null),
                new SolicitarAgendamentoComando(DOADOR, QUARTA, LocalTime.of(9, 0), null, null, USUARIO),
                new SolicitarAgendamentoComando(DOADOR, null, LocalTime.of(9, 0), null, OrigemAgendamento.DOADOR, USUARIO),
                new SolicitarAgendamentoComando(DOADOR, QUARTA, null, null, OrigemAgendamento.DOADOR, USUARIO),
                new SolicitarAgendamentoComando(DOADOR, QUARTA, LocalTime.of(9, 0), "x".repeat(501),
                        OrigemAgendamento.DOADOR, USUARIO),
                comando(LocalDate.of(2027, 3, 1), LocalTime.of(9, 0), OrigemAgendamento.DOADOR),   // fora da janela
                comando(SABADO, LocalTime.of(9, 0), OrigemAgendamento.DOADOR),                     // sem atendimento
                comando(QUARTA, LocalTime.of(12, 0), OrigemAgendamento.DOADOR),                    // fora da grade
                comando(QUARTA, LocalTime.of(9, 30), OrigemAgendamento.DOADOR)                     // fora da grade
        };
        for (SolicitarAgendamentoComando invalido : invalidos) {
            assertThatThrownBy(() -> service.solicitar(invalido)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(doadores, doacoes, impedimentos, avaliador);
        verify(agendamentos, never()).salvar(any());
    }

    @Test
    @DisplayName("doador inexistente, inativo ou que já tem agendamento ativo")
    void restricoesDoDoador() {
        when(doadores.buscarPorId(DOADOR)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.solicitar(comando(QUARTA, LocalTime.of(9, 0), OrigemAgendamento.DOADOR)))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        when(doadores.buscarPorId(DOADOR)).thenReturn(Optional.of(doador(false, "m@x.com")));
        assertThatThrownBy(() -> service.solicitar(comando(QUARTA, LocalTime.of(9, 0), OrigemAgendamento.DOADOR)))
                .isInstanceOf(ConflitoException.class);

        when(doadores.buscarPorId(DOADOR)).thenReturn(Optional.of(doador(true, "m@x.com")));
        when(agendamentos.buscarAtivoDoDoador(DOADOR)).thenReturn(Optional.of(agendamento(StatusAgendamento.PENDENTE, QUARTA)));
        assertThatThrownBy(() -> service.solicitar(comando(QUARTA, LocalTime.of(9, 0), OrigemAgendamento.DOADOR)))
                .isInstanceOf(ConflitoException.class);

        verify(agendamentos, never()).salvar(any());
    }

    @Test
    @DisplayName("doador aguardando o intervalo: a mensagem traz a data a partir da qual pode doar")
    void inelegivelAguardando() {
        doadorElegivel();
        when(avaliador.avaliarEm(any(), any(), any(), any())).thenReturn(new ResultadoElegibilidade(
                StatusElegibilidade.AGUARDANDO, List.of(MotivoInelegibilidade.INTERVALO_MINIMO),
                LocalDate.of(2026, 12, 20)));

        assertThatThrownBy(() -> service.solicitar(comando(QUARTA, LocalTime.of(9, 0), OrigemAgendamento.DOADOR)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("20/12/2026");
        verify(agendamentos, never()).salvar(any());
    }

    @Test
    @DisplayName("doador impedido: mensagem genérica, sem detalhes de saúde")
    void inelegivelImpedido() {
        doadorElegivel();
        when(avaliador.avaliarEm(any(), any(), any(), any())).thenReturn(new ResultadoElegibilidade(
                StatusElegibilidade.IMPEDIDO, List.of(MotivoInelegibilidade.IMPEDIMENTO_DEFINITIVO), null));

        assertThatThrownBy(() -> service.solicitar(comando(QUARTA, LocalTime.of(9, 0), OrigemAgendamento.DOADOR)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("O doador não está elegível para doar na data escolhida");
    }

    @Test
    @DisplayName("horário lotado (3 vagas): conflito")
    void semVagas() {
        doadorElegivel();
        when(agendamentos.contarAtivosNoHorario(QUARTA, LocalTime.of(9, 0))).thenReturn(3L);

        assertThatThrownBy(() -> service.solicitar(comando(QUARTA, LocalTime.of(9, 0), OrigemAgendamento.DOADOR)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Este horário não tem mais vagas");
        verify(agendamentos, never()).salvar(any());
    }

    @Test
    @DisplayName("falha inesperada do banco vira OperacaoDoadorException")
    void falhaDoBanco() {
        when(doadores.buscarPorId(DOADOR)).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.solicitar(comando(QUARTA, LocalTime.of(9, 0), OrigemAgendamento.DOADOR)))
                .isInstanceOf(OperacaoDoadorException.class);
    }

    // ---------- consultas do doador ----------

    @Test
    @DisplayName("listarDoDoador e ativoDoDoador exigem id e delegam ao repositório")
    void consultasDoDoador() {
        Agendamento a = agendamento(StatusAgendamento.PENDENTE, QUARTA);
        when(agendamentos.listarPorDoador(DOADOR)).thenReturn(List.of(a));
        when(agendamentos.buscarAtivoDoDoador(DOADOR)).thenReturn(Optional.of(a));

        assertThat(service.listarDoDoador(DOADOR)).containsExactly(a);
        assertThat(service.ativoDoDoador(DOADOR)).contains(a);
        assertThatThrownBy(() -> service.listarDoDoador(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.ativoDoDoador(null)).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- cancelar ----------

    @Test
    @DisplayName("doador cancela o próprio agendamento, com motivo")
    void cancelarDoDoador() {
        Agendamento a = encontrado(StatusAgendamento.PENDENTE, QUARTA);

        Agendamento cancelado = service.cancelarDoDoador(DOADOR, a.id(), "  viagem ");

        assertThat(cancelado.status()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(cancelado.motivoCancelamento()).isEqualTo("viagem");
    }

    @Test
    @DisplayName("agendamento de outro doador responde 'não encontrado'")
    void cancelarDeOutroDoador() {
        Agendamento a = encontrado(StatusAgendamento.PENDENTE, QUARTA);

        assertThatThrownBy(() -> service.cancelarDoDoador(UUID.randomUUID(), a.id(), null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(agendamentos, never()).salvar(any());
    }

    @Test
    @DisplayName("cancelar: ids obrigatórios, motivo até 300 caracteres e só agendamento ativo")
    void cancelarValidacoes() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.cancelarDoDoador(null, id, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.cancelarDoDoador(DOADOR, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.cancelarDoDoador(DOADOR, id, "x".repeat(301)))
                .isInstanceOf(IllegalArgumentException.class);

        Agendamento jaCancelado = encontrado(StatusAgendamento.CANCELADO, QUARTA);
        assertThatThrownBy(() -> service.cancelarDoDoador(DOADOR, jaCancelado.id(), null))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("equipe cancela qualquer agendamento ativo")
    void cancelarPelaEquipe() {
        Agendamento a = encontrado(StatusAgendamento.CONFIRMADO, QUARTA);

        assertThat(service.cancelarPelaEquipe(a.id(), "falta de insumo").status())
                .isEqualTo(StatusAgendamento.CANCELADO);
        assertThatThrownBy(() -> service.cancelarPelaEquipe(null, "x")).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- confirmar ----------

    @Test
    @DisplayName("confirmar grava e avisa o doador por e-mail com o nome do centro")
    void confirmar() {
        Agendamento a = encontrado(StatusAgendamento.PENDENTE, QUARTA);
        when(doadores.buscarPorId(DOADOR)).thenReturn(Optional.of(doador(true, "m@x.com")));
        when(centros.nomeDoCentroAtual()).thenReturn("Hemocentro Central");

        Agendamento confirmado = service.confirmar(a.id());

        ArgumentCaptor<AgendamentoEventosPort.DadosAgendamentoEvento> dados =
                ArgumentCaptor.forClass(AgendamentoEventosPort.DadosAgendamentoEvento.class);
        verify(eventos).confirmado(dados.capture());
        assertThat(confirmado.status()).isEqualTo(StatusAgendamento.CONFIRMADO);
        assertThat(dados.getValue().centroNome()).isEqualTo("Hemocentro Central");
        assertThat(dados.getValue().agendamentoId()).isEqualTo(a.id());
        assertThat(dados.getValue().dataHora()).isEqualTo(QUARTA.atTime(9, 0));
    }

    @Test
    @DisplayName("sem e-mail do doador, a confirmação vale mas nenhum aviso é enviado")
    void confirmarSemEmail() {
        Agendamento a = encontrado(StatusAgendamento.PENDENTE, QUARTA);
        when(doadores.buscarPorId(DOADOR)).thenReturn(Optional.of(doador(true, " ")));

        assertThat(service.confirmar(a.id()).status()).isEqualTo(StatusAgendamento.CONFIRMADO);
        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("falha ao publicar o aviso não desfaz a confirmação")
    void confirmarComFalhaNoAviso() {
        Agendamento a = encontrado(StatusAgendamento.PENDENTE, QUARTA);
        when(doadores.buscarPorId(DOADOR)).thenReturn(Optional.of(doador(true, "m@x.com")));
        doThrow(new RuntimeException("rabbit fora")).when(eventos).confirmado(any());

        assertThat(service.confirmar(a.id()).status()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    @DisplayName("confirmar: id obrigatório, inexistente e só pendente")
    void confirmarErros() {
        assertThatThrownBy(() -> service.confirmar(null)).isInstanceOf(IllegalArgumentException.class);

        UUID inexistente = UUID.randomUUID();
        when(agendamentos.buscarPorId(inexistente)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirmar(inexistente)).isInstanceOf(RecursoNaoEncontradoException.class);

        Agendamento jaConfirmado = encontrado(StatusAgendamento.CONFIRMADO, QUARTA);
        assertThatThrownBy(() -> service.confirmar(jaConfirmado.id())).isInstanceOf(ConflitoException.class);
    }

    // ---------- concluir ----------

    @Test
    @DisplayName("concluir registra a doação de hoje e liga ao agendamento")
    void concluir() {
        Agendamento a = encontrado(StatusAgendamento.CONFIRMADO, HOJE);
        UUID doacaoId = UUID.randomUUID();
        when(doacaoUseCase.registrar(any(RegistrarDoacaoComando.class)))
                .thenReturn(new Doacao(doacaoId, DOADOR, HOJE, 450, USUARIO, AGORA));

        Agendamento concluido = service.concluir(a.id(), 450, USUARIO);

        ArgumentCaptor<RegistrarDoacaoComando> comando = ArgumentCaptor.forClass(RegistrarDoacaoComando.class);
        verify(doacaoUseCase).registrar(comando.capture());
        assertThat(comando.getValue().data()).isEqualTo(HOJE);
        assertThat(comando.getValue().volumeMl()).isEqualTo(450);
        assertThat(comando.getValue().doadorId()).isEqualTo(DOADOR);
        assertThat(concluido.status()).isEqualTo(StatusAgendamento.CONCLUIDO);
        assertThat(concluido.doacaoId()).isEqualTo(doacaoId);
    }

    @Test
    @DisplayName("concluir recusa volume fora de 300 a 600 mL, agendamento futuro e não confirmado")
    void concluirRecusas() {
        Agendamento hoje = encontrado(StatusAgendamento.CONFIRMADO, HOJE);
        assertThatThrownBy(() -> service.concluir(hoje.id(), 299, USUARIO)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.concluir(hoje.id(), 601, USUARIO)).isInstanceOf(RegraDeNegocioException.class);

        Agendamento futuro = encontrado(StatusAgendamento.CONFIRMADO, QUARTA);
        assertThatThrownBy(() -> service.concluir(futuro.id(), 450, USUARIO))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("18/11/2026");

        Agendamento pendente = encontrado(StatusAgendamento.PENDENTE, HOJE);
        assertThatThrownBy(() -> service.concluir(pendente.id(), 450, USUARIO)).isInstanceOf(ConflitoException.class);

        verifyNoInteractions(doacaoUseCase);
        verify(agendamentos, never()).salvar(any());
    }

    @Test
    @DisplayName("se a doação for recusada, o agendamento não é concluído")
    void concluirComDoacaoRecusada() {
        Agendamento a = encontrado(StatusAgendamento.CONFIRMADO, HOJE);
        when(doacaoUseCase.registrar(any())).thenThrow(new RegraDeNegocioException("Doador inelegível"));

        assertThatThrownBy(() -> service.concluir(a.id(), 450, USUARIO)).isInstanceOf(RegraDeNegocioException.class);
        verify(agendamentos, never()).salvar(any());
    }

    @Test
    @DisplayName("concluir exige ids")
    void concluirIds() {
        assertThatThrownBy(() -> service.concluir(null, 450, USUARIO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.concluir(UUID.randomUUID(), 450, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- falta ----------

    @Test
    @DisplayName("registrar falta: só confirmado e a partir do dia da coleta")
    void registrarFalta() {
        Agendamento hoje = encontrado(StatusAgendamento.CONFIRMADO, HOJE);
        assertThat(service.registrarFalta(hoje.id()).status()).isEqualTo(StatusAgendamento.FALTOU);

        Agendamento futuro = encontrado(StatusAgendamento.CONFIRMADO, QUARTA);
        assertThatThrownBy(() -> service.registrarFalta(futuro.id())).isInstanceOf(RegraDeNegocioException.class);

        Agendamento pendente = encontrado(StatusAgendamento.PENDENTE, HOJE);
        assertThatThrownBy(() -> service.registrarFalta(pendente.id())).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> service.registrarFalta(null)).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- listar / agenda ----------

    @Test
    @DisplayName("listar valida filtro, intervalo de datas e limite")
    void listarValidacoes() {
        FiltroAgendamento ok = new FiltroAgendamento(HOJE, QUARTA, null, null);

        assertThatThrownBy(() -> service.listar(null, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listar(new FiltroAgendamento(QUARTA, HOJE, null, null), 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listar(ok, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listar(ok, 201)).isInstanceOf(IllegalArgumentException.class);
        verify(agendamentos, never()).listar(any(), anyInt());
    }

    @Test
    @DisplayName("listarAgenda junta nome, telefone e tipo sanguíneo do doador; doador ausente fica com nulos")
    void listarAgenda() {
        Agendamento a = agendamento(StatusAgendamento.CONFIRMADO, QUARTA);
        Agendamento semDoador = Agendamento.novo(UUID.randomUUID(), QUARTA, LocalTime.of(10, 0),
                OrigemAgendamento.EQUIPE, null, USUARIO, AGORA);
        FiltroAgendamento filtro = new FiltroAgendamento(null, null, null, null);
        when(agendamentos.listar(filtro, 50)).thenReturn(List.of(a, semDoador));
        when(doadores.buscarPorIds(any())).thenReturn(Map.of(DOADOR, doador(true, "m@x.com")));

        List<AgendamentoComDoador> agenda = service.listarAgenda(filtro, 50);

        assertThat(agenda).hasSize(2);
        assertThat(agenda.get(0).doadorNome()).isEqualTo("Maria Souza");
        assertThat(agenda.get(0).doadorTelefone()).isEqualTo("11987654321");
        assertThat(agenda.get(0).doadorTipoSanguineo()).isNotNull();
        assertThat(agenda.get(1).doadorNome()).isNull();
        assertThat(agenda.get(1).doadorTipoSanguineo()).isNull();
    }

    @Test
    @DisplayName("agenda vazia não consulta doadores")
    void agendaVazia() {
        FiltroAgendamento filtro = new FiltroAgendamento(null, null, null, null);
        when(agendamentos.listar(filtro, 50)).thenReturn(List.of());

        assertThat(service.listarAgenda(filtro, 50)).isEmpty();
        verifyNoInteractions(doadores);
    }

    @Test
    @DisplayName("buscar devolve o agendamento ou 'não encontrado'")
    void buscar() {
        Agendamento a = encontrado(StatusAgendamento.PENDENTE, QUARTA);
        UUID inexistente = UUID.randomUUID();
        when(agendamentos.buscarPorId(inexistente)).thenReturn(Optional.empty());

        assertThat(service.buscar(a.id())).isEqualTo(a);
        assertThatThrownBy(() -> service.buscar(inexistente)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.buscar(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
