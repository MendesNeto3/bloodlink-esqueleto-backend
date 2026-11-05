package com.bloodlink.api.agendamento.adapter.in.web.dto;

import com.bloodlink.api.agendamento.domain.HorariosDoDia;
import com.bloodlink.api.agendamento.domain.Item;

import java.time.LocalDate;
import java.util.List;

public record HorariosResponse(LocalDate data, List<Item> horarios) {

    public static HorariosResponse de(HorariosDoDia dia) {
        return new HorariosResponse(dia.data(), dia.horarios().stream()
                .map(h -> new Item(h.hora().toString(), h.disponivel()))
                .toList());
    }
}
