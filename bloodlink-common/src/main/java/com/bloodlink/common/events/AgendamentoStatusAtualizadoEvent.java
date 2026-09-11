package com.bloodlink.common.events;

import java.util.UUID;

public record AgendamentoStatusAtualizadoEvent(
        UUID agendamentoId,
        String centroSchema,
        String status
) {}