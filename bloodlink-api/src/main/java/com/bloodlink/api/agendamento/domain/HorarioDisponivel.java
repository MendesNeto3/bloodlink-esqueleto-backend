package com.bloodlink.api.agendamento.domain;

import java.time.LocalTime;

public record HorarioDisponivel(LocalTime hora, boolean disponivel) {
}
