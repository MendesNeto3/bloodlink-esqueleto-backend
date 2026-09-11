package com.bloodlink.common.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record LembreteDoacaoEvent(
        UUID agendamentoId,
        String centroSchema,
        String centroNome,
        UUID doadorId,
        String doadorNome,
        String doadorEmail,
        LocalDateTime dataHora
) {}