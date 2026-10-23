package com.bloodlink.api.doador.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

public record RegistrarDoacaoComando(UUID doadorId, LocalDate data, int volumeMl, UUID registradaPor) {
}
