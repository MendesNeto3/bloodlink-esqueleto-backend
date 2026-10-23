package com.bloodlink.api.doador.adapter.in.web.dto;

import com.bloodlink.api.doador.application.service.FichaPreDoacao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FichaPreDoacaoResponse(UUID doadorId, boolean bemDeSaude, boolean dormiu6h,
                                     boolean alimentadoHidratado, boolean alcool12h,
                                     boolean procedimentoRecente, boolean gestanteAmamentando,
                                     boolean doencaRelevante, boolean medicamentoOuVacina,
                                     String observacoes, Instant atualizadaEm, List<String> alertas) {

    public static FichaPreDoacaoResponse de(FichaPreDoacao f) {
        return new FichaPreDoacaoResponse(f.doadorId(), f.bemDeSaude(), f.dormiu6h(), f.alimentadoHidratado(),
                f.alcool12h(), f.procedimentoRecente(), f.gestanteAmamentando(), f.doencaRelevante(),
                f.medicamentoOuVacina(), f.observacoes(), f.atualizadaEm(), f.alertas());
    }
}
