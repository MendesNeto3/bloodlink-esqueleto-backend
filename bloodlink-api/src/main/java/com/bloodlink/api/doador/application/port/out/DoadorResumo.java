package com.bloodlink.api.doador.application.port.out;

import com.bloodlink.api.doador.domain.StatusElegibilidade;
import com.bloodlink.api.doador.domain.TipoSanguineo;

import java.time.LocalDate;
import java.util.UUID;

public record DoadorResumo(
        UUID id,
        String nome,
        TipoSanguineo tipoSanguineo,
        boolean ativo,
        LocalDate ultimaDoacao,
        int totalDoacoes,
        StatusElegibilidade status,
        LocalDate elegivelEm
) {

    /** Nome é dado pessoal: não sai em log. */
    @Override
    public String toString() {
        return "DoadorResumo[id=" + id + "]";
    }
}
