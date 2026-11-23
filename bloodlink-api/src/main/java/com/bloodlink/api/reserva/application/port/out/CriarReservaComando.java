package com.bloodlink.api.reserva.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

public record CriarReservaComando(String hospital, String tipoSanguineo, Integer unidades,
                                  LocalDate validaAte, UUID criadoPor) {
}
