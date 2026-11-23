package com.bloodlink.api.agendamento.adapter.in.web.dto;

import com.bloodlink.api.agendamento.domain.Agendamento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AgendaAgendamentoResponse(UUID id, String codigo, UUID doadorId, String doadorNome,
                                        String doadorTelefone, String doadorTipoSanguineo, LocalDate data,
                                        String hora, String status, String origem, String observacao,
                                        String motivoCancelamento, Instant criadoEm, String centroNome) {

    public static AgendaAgendamentoResponse de(AgendamentoComDoador item, String centroNome) {
        Agendamento a = item.agendamento();
        return new AgendaAgendamentoResponse(a.id(), a.codigo(), a.doadorId(), item.doadorNome(),
                item.doadorTelefone(), item.doadorTipoSanguineo(), a.date(), a.hora().toString(),
                a.status().name(), a.origem().name(), a.observacao(), a.motivoCancelamento(), a.criadoEm(),
                centroNome);
    }
}
