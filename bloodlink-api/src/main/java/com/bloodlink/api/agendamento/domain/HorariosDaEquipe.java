package com.bloodlink.api.agendamento.domain;

import java.time.LocalDate;
import java.util.List;

public record HorariosDaEquipe(LocalDate data, List<HorarioComVagas> horarios) {}
