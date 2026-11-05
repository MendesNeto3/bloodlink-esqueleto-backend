package com.bloodlink.api.agendamento.application.usecase;

import com.bloodlink.api.agendamento.domain.Agendamento;
import com.bloodlink.api.agendamento.domain.FiltroAgendamento;
import com.bloodlink.api.agendamento.domain.SolicitarAgendamentoComando;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgendamentoUseCase {

    Agendamento solicitar(SolicitarAgendamentoComando comando);

    List<Agendamento> listarDoDoador(UUID doadorId);

    Optional<Agendamento> ativoDoDoador(UUID doadorId);

    /** Só cancela se o agendamento for do próprio doador; senão responde como "não encontrado". */
    Agendamento cancelarDoDoador(UUID doadorId, UUID agendamentoId, String motivo);

    Agendamento buscar(UUID agendamentoId);

    List<Agendamento> listar(FiltroAgendamento filtro, int limite);

    Agendamento confirmar(UUID agendamentoId);

    Agendamento cancelarPelaEquipe(UUID agendamentoId, String motivo);
}
