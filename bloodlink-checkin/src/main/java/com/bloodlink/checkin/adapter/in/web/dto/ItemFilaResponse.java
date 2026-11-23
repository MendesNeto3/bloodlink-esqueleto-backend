package com.bloodlink.checkin.adapter.in.web.dto;

import com.bloodlink.checkin.domain.ItemFila;

import java.time.Instant;
import java.util.UUID;

/** A observação da triagem (motivo clínico) é dado de saúde e não sai pela API. */
public record ItemFilaResponse(UUID agendamentoId,
                               UUID checkinId,
                               UUID doadorId,
                               String doadorNome,
                               String tipoSanguineo,
                               String tipoSanguineoRotulo,
                               String hora,
                               String statusAgendamento,
                               String situacao,
                               Instant chegadaEm,
                               String triagem,
                               Instant triagemEm) {

    public static ItemFilaResponse de(ItemFila i) {
        return new ItemFilaResponse(i.agendamentoId(), i.checkinId(), i.doadorId(), i.doadorNome(),
                i.tipoSanguineo(), rotulo(i.tipoSanguineo()), i.hora().toString(), i.statusAgendamento(),
                i.situacao().name(), i.chegadaEm(), i.triagem() == null ? null : i.triagem().name(),
                i.triagemEm());
    }

    /** A_POSITIVO vira A+, AB_NEGATIVO vira AB-. */
    private static String rotulo(String tipo) {
        return tipo == null ? null : tipo.replace("_POSITIVO", "+").replace("_NEGATIVO", "-");
    }
}
