package com.bloodlink.api.agendamento.domain;

import com.bloodlink.api.agendamento.domain.enums.OrigemAgendamento;
import com.bloodlink.api.agendamento.domain.enums.StatusAgendamento;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgendamentoTest {

    private static final Instant T0 = Instant.parse("2026-11-17T15:00:00Z");
    private static final Instant T1 = T0.plusSeconds(120);

    private Agendamento pendente() {
        return Agendamento.novo(UUID.randomUUID(), LocalDate.of(2026, 11, 20), LocalTime.of(9, 30),
                OrigemAgendamento.DOADOR, "obs", UUID.randomUUID(), T0);
    }

    private Agendamento confirmado() {
        return pendente().confirmar(T0);
    }

    @Test
    @DisplayName("novo agendamento nasce PENDENTE, sem motivo nem doação")
    void novo() {
        Agendamento a = pendente();

        assertThat(a.status()).isEqualTo(StatusAgendamento.PENDENTE);
        assertThat(a.motivoCancelamento()).isNull();
        assertThat(a.doacaoId()).isNull();
        assertThat(a.criadoEm()).isEqualTo(T0);
        assertThat(a.atualizadoEm()).isEqualTo(T0);
    }

    @Test
    @DisplayName("confirmar: PENDENTE vira CONFIRMADO e atualiza o instante")
    void confirmar() {
        Agendamento a = pendente().confirmar(T1);

        assertThat(a.status()).isEqualTo(StatusAgendamento.CONFIRMADO);
        assertThat(a.atualizadoEm()).isEqualTo(T1);
        assertThat(a.criadoEm()).isEqualTo(T0);
    }

    @Test
    @DisplayName("confirmar só vale para pendente")
    void confirmarInvalido() {
        assertThatThrownBy(() -> confirmado().confirmar(T1)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> pendente().cancelar("x", T1).confirmar(T1)).isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("cancelar: vale para pendente e confirmado, e guarda o motivo")
    void cancelar() {
        Agendamento daPendente = pendente().cancelar("imprevisto", T1);
        Agendamento daConfirmada = confirmado().cancelar("viagem", T1);

        assertThat(daPendente.status()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(daPendente.motivoCancelamento()).isEqualTo("imprevisto");
        assertThat(daConfirmada.status()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(daConfirmada.motivoCancelamento()).isEqualTo("viagem");
    }

    @Test
    @DisplayName("não cancela o que já está cancelado, concluído ou faltou")
    void cancelarInvalido() {
        UUID doacao = UUID.randomUUID();
        assertThatThrownBy(() -> pendente().cancelar("a", T1).cancelar("b", T1)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> confirmado().concluir(doacao, T1).cancelar("b", T1))
                .isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> confirmado().registrarFalta(T1).cancelar("b", T1))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("concluir: CONFIRMADO vira CONCLUIDO e liga a doação")
    void concluir() {
        UUID doacao = UUID.randomUUID();

        Agendamento a = confirmado().concluir(doacao, T1);

        assertThat(a.status()).isEqualTo(StatusAgendamento.CONCLUIDO);
        assertThat(a.doacaoId()).isEqualTo(doacao);
    }

    @Test
    @DisplayName("concluir exige agendamento confirmado e doação informada")
    void concluirInvalido() {
        UUID doacao = UUID.randomUUID();
        assertThatThrownBy(() -> pendente().concluir(doacao, T1)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> confirmado().concluir(null, T1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("registrar falta: só para confirmado, sem doação ligada")
    void falta() {
        Agendamento a = confirmado().registrarFalta(T1);

        assertThat(a.status()).isEqualTo(StatusAgendamento.FALTOU);
        assertThat(a.doacaoId()).isNull();
        assertThatThrownBy(() -> pendente().registrarFalta(T1)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> a.registrarFalta(T1)).isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("transições preservam os demais campos")
    void preservaCampos() {
        Agendamento original = pendente();

        Agendamento confirmado = original.confirmar(T1);

        assertThat(confirmado.id()).isEqualTo(original.id());
        assertThat(confirmado.doadorId()).isEqualTo(original.doadorId());
        assertThat(confirmado.date()).isEqualTo(original.date());
        assertThat(confirmado.hora()).isEqualTo(original.hora());
        assertThat(confirmado.origem()).isEqualTo(OrigemAgendamento.DOADOR);
        assertThat(confirmado.observacao()).isEqualTo("obs");
        assertThat(confirmado.solicitadoPor()).isEqualTo(original.solicitadoPor());
    }

    @Test
    @DisplayName("status ativo = pendente ou confirmado")
    void ativo() {
        assertThat(StatusAgendamento.PENDENTE.ativo()).isTrue();
        assertThat(StatusAgendamento.CONFIRMADO.ativo()).isTrue();
        assertThat(StatusAgendamento.CONCLUIDO.ativo()).isFalse();
        assertThat(StatusAgendamento.CANCELADO.ativo()).isFalse();
        assertThat(StatusAgendamento.FALTOU.ativo()).isFalse();
    }

    @Test
    @DisplayName("código AG- deriva do id; toString mostra só o id")
    void codigoEToString() {
        Agendamento a = pendente();

        assertThat(a.codigo()).matches("AG-[0-9A-F]{8}");
        assertThat(a.codigo()).isEqualTo("AG-" + a.id().toString().substring(0, 8).toUpperCase());
        assertThat(a.toString()).isEqualTo("Agendamento[id=" + a.id() + "]");
    }
}
