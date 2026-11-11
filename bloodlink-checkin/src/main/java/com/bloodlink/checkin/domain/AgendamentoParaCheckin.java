package com.bloodlink.checkin.domain;

import java.time.LocalDate;
import java.util.UUID;

/** O que o check-in precisa saber do agendamento (que pertence à API). */
public record AgendamentoParaCheckin(UUID id, UUID doadorId, LocalDate data, String status) {
}