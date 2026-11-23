package com.bloodlink.api.agendamento.adapter.in.web.dto;

import com.bloodlink.api.agendamento.application.usecase.LembreteDoacaoUseCase.ResultadoLembretes;

public record LembretesResponse(int enviados, int semEmail, int falhas) {

    public static LembretesResponse de(ResultadoLembretes r) {
        return new LembretesResponse(r.enviados(), r.semEmail(), r.falhas());
    }
}
