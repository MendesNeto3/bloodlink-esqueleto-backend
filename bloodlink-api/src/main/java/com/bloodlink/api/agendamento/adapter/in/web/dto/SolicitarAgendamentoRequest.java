package com.bloodlink.api.agendamento.adapter.in.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SolicitarAgendamentoRequest(LocalDate data, LocalTime hora, String observacao) {
}