package com.bloodlink.api.agendamento.adapter.in.web.dto;

import com.bloodlink.api.agendamento.domain.HorariosDaEquipe;

import java.time.LocalDate;
import java.util.List;

public record HorariosEquipeResponse(LocalDate data, List<Item> horarios) {

    public record Item(String hora, boolean disponivel, long vagasRestantes, int vagasTotais) {}

    public static HorariosEquipeResponse de(HorariosDaEquipe dia) {
        return new HorariosEquipeResponse(dia.data(), dia.horarios().stream()
                .map(h -> new Item(h.hora().toString(), h.disponivel(), h.vagasRestantes(), h.vagasTotais()))
                .toList());
    }
}
