package com.bloodlink.checkin.adapter.in.web.dto;

import com.bloodlink.checkin.domain.Checkin;
import com.bloodlink.checkin.domain.DetalheCheckin;

import java.time.Instant;
import java.util.UUID;

/** A observação da triagem (motivo clínico) é dado de saúde e não sai pela API. */
public record CheckinResponse(UUID id,
                              UUID agendamentoId,
                              UUID doadorId,
                              Instant chegadaEm,
                              String situacao,
                              String triagem,
                              Instant triagemEm) {

    public static CheckinResponse de(DetalheCheckin d) {
        Checkin c = d.checkin();
        return new CheckinResponse(c.id(), c.agendamentoId(), c.doadorId(), c.chegadaEm(),
                d.situacao().name(), c.triagem() == null ? null : c.triagem().name(), c.triagemEm());
    }
}
