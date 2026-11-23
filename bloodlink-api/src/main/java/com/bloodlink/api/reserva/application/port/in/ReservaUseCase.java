package com.bloodlink.api.reserva.application.port.in;

import com.bloodlink.api.reserva.application.port.out.CriarReservaComando;
import com.bloodlink.api.reserva.domain.ReservaComSituacao;
import com.bloodlink.api.reserva.domain.SituacaoReserva;

import java.util.List;
import java.util.UUID;

public interface ReservaUseCase {
    ReservaComSituacao criar(CriarReservaComando comando);

    /** situacao nula traz todas. */
    List<ReservaComSituacao> listar(SituacaoReserva situacao, int limite);

    ReservaComSituacao liberar(UUID id);
}
