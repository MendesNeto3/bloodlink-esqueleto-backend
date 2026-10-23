package com.bloodlink.api.doador.application.port.out;

import com.bloodlink.api.doador.domain.Doacao;

import java.time.LocalDate;
import java.util.UUID;

public record DoacaoResponse(UUID id, LocalDate data, int volumeMl) {

    public static DoacaoResponse de(Doacao d) {
        return new DoacaoResponse(d.id(), d.data(), d.volumeMl());
    }
}
