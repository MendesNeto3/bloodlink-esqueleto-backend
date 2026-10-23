package com.bloodlink.api.doador.application.port.out;


import java.time.LocalDate;
import java.util.UUID;

public record ImpedimentoResponse(
        UUID id,
        boolean definitivo,
        LocalDate inicio,
        LocalDate fim,
        String motivo,
        boolean encerrado
) {

    public static ImpedimentoResponse de(Impedimento i) {
        return new ImpedimentoResponse(i.id(), i.definitivo(), i.inicio(), i.fim(), i.motivo(),
                i.encerradoEm() != null);
    }

    @Override
    public String toString() {
        return "ImpedimentoResponse[id=" + id + "]";
    }
}
