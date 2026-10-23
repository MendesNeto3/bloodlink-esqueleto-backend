package com.bloodlink.api.doador.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

public record RegistrarImpedimentoRequest(Boolean definitivo, LocalDate inicio, LocalDate fim, String motivo) {

    public RegistrarImpedimentoComando paraComando(UUID doadorId, UUID usuarioId) {
        return new RegistrarImpedimentoComando(doadorId, Boolean.TRUE.equals(definitivo), inicio, fim,
                motivo, usuarioId);
    }
}
