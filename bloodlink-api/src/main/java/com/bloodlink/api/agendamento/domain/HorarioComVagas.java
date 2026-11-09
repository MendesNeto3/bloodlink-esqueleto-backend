package com.bloodlink.api.agendamento.domain;

import java.time.LocalTime;

public record HorarioComVagas(LocalTime hora, int vagasTotais, long ocupadas) {
    public long vagasRestantes() {
        return Math.max(0, vagasTotais - ocupadas);
    }

    public boolean disponivel() {
        return ocupadas < vagasTotais;
    }
}
