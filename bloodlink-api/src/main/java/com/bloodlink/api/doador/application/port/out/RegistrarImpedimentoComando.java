package com.bloodlink.api.doador.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

public record RegistrarImpedimentoComando(
        UUID doadorId,
        boolean definitivo,
        LocalDate inicio,
        LocalDate fim,
        String motivo,
        UUID registradoPor
) {

    /** O motivo é dado de saúde: não sai em log. */
    @Override
    public String toString() {
        return "RegistrarImpedimentoComando[doadorId=" + doadorId + "]";
    }
}