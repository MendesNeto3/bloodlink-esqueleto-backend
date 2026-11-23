package com.bloodlink.api.reserva.adapter.in.web.dto;

import java.time.LocalDate;

public record ReservaRequest(String hospital, String tipoSanguineo, Integer unidades, LocalDate validaAte) {
}
