package com.bloodlink.api.agendamento.adapter.in.web.dto;

import com.bloodlink.api.agendamento.domain.Agendamento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** centroNome só vem preenchido nas listas; nas demais rotas fica nulo. */
public record AgendamentoResponse(UUID id, String codigo, UUID doadorId, LocalDate date, String hora, String status,
                                  String origem, String observacao, String motivoCancelamento, Instant criadoEm,
                                  String centroNome) {

    public static AgendamentoResponse de(Agendamento a) {
        return de(a, null);
    }

    public static AgendamentoResponse de(Agendamento a, String centroNome) {
        return new AgendamentoResponse(a.id(), a.codigo(), a.doadorId(), a.date(), a.hora().toString(),
                a.status().name(), a.origem().name(), a.observacao(), a.motivoCancelamento(), a.criadoEm(),
                centroNome);
    }
}
