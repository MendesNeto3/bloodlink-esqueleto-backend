package com.bloodlink.api.agendamento.adapter.in.web.dto;

import com.bloodlink.api.agendamento.domain.Agendamento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AgendamentoResponse(UUID id, UUID doadorId, LocalDate date, String hora, String status,
                                  String origem, String observacao, String motivoCancelamento, Instant criadoEm) {

    public static AgendamentoResponse de(Agendamento a) {
        return new AgendamentoResponse(a.id(), a.doadorId(), a.date(), a.hora().toString(), a.status().name(),
                a.origem().name(), a.observacao(), a.motivoCancelamento(), a.criadoEm());
    }
}
