package com.bloodlink.api.agendamento.adapter.in.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AgendarPelaEquipeRequest(UUID doadorId, LocalDate data, LocalTime hora, String observacao) {}
