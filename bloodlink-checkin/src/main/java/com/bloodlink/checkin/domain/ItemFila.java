package com.bloodlink.checkin.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record ItemFila(UUID agendamentoId,
                       UUID checkinId,
                       UUID doadorId,
                       String doadorNome,
                       String tipoSanguineo,
                       LocalTime hora,
                       String statusAgendamento,
                       SituacaoCheckin situacao,
                       Instant chegadaEm,
                       ResultadoTriagem triagem,
                       String triagemObservacao,
                       Instant triagemEm) {

    @Override
    public String toString() {
        return "ItemFila[agendamentoId=" + agendamentoId + "]";
    }
}