package com.bloodlink.checkin.domain;

/** O check-in junto com a situação real, que depende também do status do agendamento. */
public record DetalheCheckin(Checkin checkin, SituacaoCheckin situacao) {
}
