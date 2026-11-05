package com.bloodlink.api.agendamento.application.port.in;


import com.bloodlink.api.agendamento.domain.Agendamento;
import com.bloodlink.api.agendamento.domain.FiltroAgendamento;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AgendamentoRepositoryPort {

    Agendamento salvar(Agendamento agendamento);

    Optional<Agendamento> buscarPorId(UUID id);

    /** Pendente ou confirmado: no máximo um por doador. */
    Optional<Agendamento> buscarAtivoDoDoador(UUID doadorId);

    /** Do mais recente para o mais antigo. */
    List<Agendamento> listarPorDoador(UUID doadorId);

    long contarAtivosNoHorario(LocalDate data, LocalTime hora);

    /** Vagas ocupadas por horário num dia, em uma consulta só. */
    Map<LocalTime, Long> contarAtivosPorHorario(LocalDate data);

    List<Agendamento> listar(FiltroAgendamento filtro, int limite);
}