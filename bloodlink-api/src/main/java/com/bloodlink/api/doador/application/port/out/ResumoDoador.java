package com.bloodlink.api.doador.application.port.out;

import com.bloodlink.api.doador.domain.Doador;

import java.time.LocalDate;

public record ResumoDoador(Doador doador,
                           int totalDoacoes,
                           LocalDate ultimaDoacao,
                           ResultadoElegibilidade elegibilidade) {
}