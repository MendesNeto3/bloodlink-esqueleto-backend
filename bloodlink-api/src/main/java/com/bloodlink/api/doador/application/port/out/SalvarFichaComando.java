package com.bloodlink.api.doador.application.port.out;

import com.bloodlink.api.doador.application.service.FichaPreDoacao;

import java.time.Instant;
import java.util.UUID;

public record SalvarFichaComando(UUID doadorId, boolean bemDeSaude, boolean dormiu6h,
                                 boolean alimentadoHidratado, boolean alcool12h, boolean procedimentoRecente,
                                 boolean gestanteAmamentando, boolean doencaRelevante,
                                 boolean medicamentoOuVacina, String observacoes) {

    public FichaPreDoacao paraFicha(Instant agora) {
        return new FichaPreDoacao(doadorId, bemDeSaude, dormiu6h, alimentadoHidratado, alcool12h,
                procedimentoRecente, gestanteAmamentando, doencaRelevante, medicamentoOuVacina,
                observacoes, agora);
    }
}