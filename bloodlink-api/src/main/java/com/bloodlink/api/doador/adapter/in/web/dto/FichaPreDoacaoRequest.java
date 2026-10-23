package com.bloodlink.api.doador.adapter.in.web.dto;

import com.bloodlink.api.doador.application.port.out.SalvarFichaComando;

import java.util.UUID;

public record FichaPreDoacaoRequest(Boolean bemDeSaude, Boolean dormiu6h, Boolean alimentadoHidratado,
                                    Boolean alcool12h, Boolean procedimentoRecente,
                                    Boolean gestanteAmamentando, Boolean doencaRelevante,
                                    Boolean medicamentoOuVacina, String observacoes) {

    public SalvarFichaComando paraComando(UUID doadorId) {
        if (bemDeSaude == null || dormiu6h == null || alimentadoHidratado == null || alcool12h == null
                || procedimentoRecente == null || gestanteAmamentando == null
                || doencaRelevante == null || medicamentoOuVacina == null) {
            throw new IllegalArgumentException("Responda todas as perguntas da ficha");
        }
        return new SalvarFichaComando(doadorId, bemDeSaude, dormiu6h, alimentadoHidratado, alcool12h,
                procedimentoRecente, gestanteAmamentando, doencaRelevante, medicamentoOuVacina, observacoes);
    }
}
