package com.bloodlink.checkin.application.port.out;

import com.bloodlink.checkin.domain.AgendamentoParaCheckin;
import com.bloodlink.checkin.domain.Checkin;
import com.bloodlink.checkin.domain.ItemFila;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinRepositoryPort {

    /** Trava a linha do agendamento (for share) enquanto a transação durar. */
    Optional<AgendamentoParaCheckin> buscarAgendamento(UUID id);

    /** false se o agendamento já tem check-in (garantido pelo banco, mesmo com duas chamadas ao mesmo tempo). */
    boolean inserir(Checkin checkin);

    Optional<Checkin> buscarPorId(UUID id);

    /** Trava a linha do check-in (for update) enquanto a transação durar. */
    Optional<Checkin> buscarPorIdParaAtualizar(UUID id);

    /** false se a triagem já tinha sido registrada. */
    boolean atualizarTriagem(Checkin checkin);

    List<ItemFila> fila(LocalDate data);
}