package com.bloodlink.checkin.application.port.in;

import com.bloodlink.checkin.domain.Checkin;
import com.bloodlink.checkin.domain.ResumoFila;

import java.time.LocalDate;
import java.util.UUID;

public interface CheckinUseCase {

    Checkin registrarChegada(UUID agendamentoId, UUID registradoPor);

    Checkin registrarTriagem(UUID checkinId, String resultado, String observacao, UUID triadoPor);

    Checkin buscar(UUID id);

    /** data null = hoje; situacao null ou em branco = todas. */
    ResumoFila fila(LocalDate data, String situacao);
}