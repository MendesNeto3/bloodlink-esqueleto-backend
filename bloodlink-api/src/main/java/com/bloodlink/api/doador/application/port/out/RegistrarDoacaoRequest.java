package com.bloodlink.api.doador.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

public record RegistrarDoacaoRequest(LocalDate data, Integer volumeMl) {

    public RegistrarDoacaoComando paraComando(UUID doadorId, UUID usuarioId) {
        // volume ausente vira 0 e o serviço recusa (422), em vez de estourar NullPointerException
        return new RegistrarDoacaoComando(doadorId, data, volumeMl == null ? 0 : volumeMl, usuarioId);
    }
}
