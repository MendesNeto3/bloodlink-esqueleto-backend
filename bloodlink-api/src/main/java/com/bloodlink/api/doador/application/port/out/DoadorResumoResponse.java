package com.bloodlink.api.doador.application.port.out;

import com.bloodlink.api.doador.domain.StatusElegibilidade;

import java.time.LocalDate;
import java.util.UUID;

public record DoadorResumoResponse(
        UUID id,
        String nome,
        String tipoSanguineo,
        boolean ativo,
        LocalDate ultimaDoacao,
        int totalDoacoes,
        StatusElegibilidade status,
        LocalDate elegivelEm
) {

    public static DoadorResumoResponse de(DoadorResumo r) {
        return new DoadorResumoResponse(r.id(), r.nome(), r.tipoSanguineo().rotulo(), r.ativo(),
                r.ultimaDoacao(), r.totalDoacoes(), r.status(), r.elegivelEm());
    }
}
