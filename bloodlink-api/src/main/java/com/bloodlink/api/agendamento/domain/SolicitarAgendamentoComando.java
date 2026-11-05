package com.bloodlink.api.agendamento.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SolicitarAgendamentoComando(UUID doadorId, LocalDate data, LocalTime hora, String observacao,
                                          OrigemAgendamento origem, UUID solicitadoPor) {
}
