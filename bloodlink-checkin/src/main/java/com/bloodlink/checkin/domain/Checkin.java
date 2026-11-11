package com.bloodlink.checkin.domain;

import java.time.Instant;
import java.util.UUID;

public record Checkin(UUID id,
                      UUID agendamentoId,
                      UUID doadorId,
                      Instant chegadaEm,
                      UUID registradoPor,
                      ResultadoTriagem triagem,
                      String triagemObservacao,
                      Instant triagemEm,
                      UUID triagemPor) {

    public static Checkin novo(UUID agendamentoId, UUID doadorId, UUID registradoPor, Instant agora) {
        return new Checkin(UUID.randomUUID(), agendamentoId, doadorId, agora, registradoPor,
                null, null, null, null);
    }

    public boolean triado() {
        return triagem != null;
    }

    public Checkin comTriagem(ResultadoTriagem resultado, String observacao, UUID triadoPor, Instant agora) {
        return new Checkin(id, agendamentoId, doadorId, chegadaEm, registradoPor,
                resultado, observacao, agora, triadoPor);
    }

    public SituacaoCheckin situacao() {
        if (triagem == null) {
            return SituacaoCheckin.AGUARDANDO_TRIAGEM;
        }
        return triagem == ResultadoTriagem.APTA ? SituacaoCheckin.APTO : SituacaoCheckin.INAPTO;
    }

    /** A observação da triagem é dado de saúde (LGPD): nunca vai para log. */
    @Override
    public String toString() {
        return "Checkin[id=" + id + "]";
    }
}