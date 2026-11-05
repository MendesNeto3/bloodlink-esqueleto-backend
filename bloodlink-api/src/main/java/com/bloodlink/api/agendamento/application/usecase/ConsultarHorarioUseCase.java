package com.bloodlink.api.agendamento.application.usecase;

import com.bloodlink.api.agendamento.domain.HorariosDoDia;

import java.time.LocalDate;

public interface ConsultarHorarioUseCase {
    HorariosDoDia consultar(LocalDate data);
}
