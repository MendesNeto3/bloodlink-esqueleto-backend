package com.bloodlink.api.doador.application.port.out;

import com.bloodlink.api.doador.domain.MotivoInelegibilidade;
import com.bloodlink.api.doador.domain.StatusElegibilidade;

import java.time.LocalDate;
import java.util.List;

public record ResultadoElegibilidade(
        StatusElegibilidade status,
        List<MotivoInelegibilidade> motivos,
        LocalDate elegivelEm
) {

    public ResultadoElegibilidade {
        motivos = List.copyOf(motivos);

    }
}