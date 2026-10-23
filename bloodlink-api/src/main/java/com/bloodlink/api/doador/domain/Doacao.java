package com.bloodlink.api.doador.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Doacao(
        UUID id,
        UUID doadorId,
        LocalDate data,
        int volumeMl,
        UUID registradaPor,
        Instant criadaEm
) {

    public Doacao {
        if (data == null) {
            throw new IllegalArgumentException("A data da doação é obrigatória");
        }
        if (volumeMl < 300 || volumeMl > 600) {
            throw new IllegalArgumentException("O volume da doação deve estar entre 300 e 600 mL");
        }
    }
}
