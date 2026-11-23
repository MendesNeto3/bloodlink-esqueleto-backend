package com.bloodlink.api.reserva.adapter.in.web.dto;

import com.bloodlink.api.reserva.domain.Reserva;
import com.bloodlink.api.reserva.domain.ReservaComSituacao;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReservaResponse(UUID id, String codigo, String hospital, String tipoSanguineo, int unidades,
                              LocalDate validaAte, String situacao, Instant criadoEm, Instant liberadaEm) {

    public static ReservaResponse de(ReservaComSituacao item) {
        Reserva r = item.reserva();
        return new ReservaResponse(r.id(), r.codigo(), r.hospital(), r.tipoSanguineo().rotulo(), r.unidades(),
                r.validaAte(), item.situacao().name(), r.criadoEm(), r.liberadaEm());
    }
}
