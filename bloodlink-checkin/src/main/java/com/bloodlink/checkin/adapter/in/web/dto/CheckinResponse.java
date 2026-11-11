package com.bloodlink.checkin.adapter.in.web.dto;

import com.bloodlink.checkin.domain.Checkin;

import java.time.Instant;
import java.util.UUID;

public record CheckinResponse(UUID id,
                              UUID agendamentoId,
                              UUID doadorId,
                              Instant chegadaEm,
                              String situacao,
                              String triagem,
                              String triagemObservacao,
                              Instant triagemEm) {

    public static CheckinResponse de(Checkin c) {
        return new CheckinResponse(c.id(), c.agendamentoId(), c.doadorId(), c.chegadaEm(),
                c.situacao().name(), c.triagem() == null ? null : c.triagem().name(),
                c.triagemObservacao(), c.triagemEm());
    }
}