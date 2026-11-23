package com.bloodlink.checkin.application.port.in;

import com.bloodlink.checkin.domain.DetalheCheckin;
import com.bloodlink.checkin.domain.ResumoFila;

import java.time.LocalDate;
import java.util.UUID;

public interface CheckinUseCase {

    DetalheCheckin registrarChegada(UUID agendamentoId, UUID registradoPor);

    DetalheCheckin registrarTriagem(UUID checkinId, String resultado, String observacao, UUID triadoPor);

    DetalheCheckin buscar(UUID id);

    /** data null = hoje; situacao null ou em branco = todas. */
    ResumoFila fila(LocalDate data, String situacao);
}
