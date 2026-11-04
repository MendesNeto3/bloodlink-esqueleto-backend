package com.bloodlink.api.agendamento.domain;

import java.time.LocalDate;
import java.util.UUID;

public record FiltroAgendamento(LocalDate de, LocalDate ate, StatusAgendamento status, UUID doadorId) {
}
