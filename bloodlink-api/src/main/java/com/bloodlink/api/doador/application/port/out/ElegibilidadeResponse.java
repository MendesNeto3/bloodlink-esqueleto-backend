package com.bloodlink.api.doador.application.port.out;

import com.bloodlink.api.doador.domain.MotivoInelegibilidade;
import com.bloodlink.api.doador.domain.StatusElegibilidade;

import java.time.LocalDate;
import java.util.List;

public record ElegibilidadeResponse(
        StatusElegibilidade status,
        List<MotivoInelegibilidade> motivos,
        LocalDate elegivelEm
) {

    public static ElegibilidadeResponse de(ResultadoElegibilidade r){
        return new ElegibilidadeResponse(r.status(), r.motivos(), r.elegivelEm());
    }
}
