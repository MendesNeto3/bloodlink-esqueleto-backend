package com.bloodlink.common.events;

import java.util.UUID;

public record CentroRegistradoEvent(
        String schema,
        UUID centroId,
        boolean ativo
) {}