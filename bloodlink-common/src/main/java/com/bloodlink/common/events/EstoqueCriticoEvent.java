package com.bloodlink.common.events;

import java.time.Instant;

/** Um tipo sanguíneo passou a ficar no limiar crítico (ou abaixo) depois de uma baixa. */
public record EstoqueCriticoEvent(
        String centroSchema,
        String tipoSanguineo,
        String rotulo,
        long unidades,
        int limiarCritico,
        Instant ocorridoEm
) {}