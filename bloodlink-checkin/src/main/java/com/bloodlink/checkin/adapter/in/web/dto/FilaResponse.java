package com.bloodlink.checkin.adapter.in.web.dto;

import com.bloodlink.checkin.domain.ResumoFila;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FilaResponse(LocalDate data,
                           int total,
                           Map<String, Long> totais,
                           List<ItemFilaResponse> itens) {

    public static FilaResponse de(ResumoFila r) {
        Map<String, Long> totais = new LinkedHashMap<>();
        r.totais().forEach((situacao, quantidade) -> totais.put(situacao.name(), quantidade));
        List<ItemFilaResponse> itens = r.itens().stream().map(ItemFilaResponse::de).toList();
        return new FilaResponse(r.data(), itens.size(), totais, itens);
    }
}