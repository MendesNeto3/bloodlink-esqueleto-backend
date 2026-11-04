package com.bloodlink.api.agendamento.domain;

import java.time.LocalDate;
import java.util.List;

public record HorariosDoDia(LocalDate data, List<HorarioDisponivel> horarios) {
}
