package com.bloodlink.api.doador.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public record AtualizarPerfilDoadorCommand(UUID doadorId, String telefone, String cidade, String uf,
                                           BigDecimal pesoKg) {
    @Override
    public String toString() {
        return "AtualizarPerfilDoadorCommand[doadorId=" + doadorId + "]";
    }
}

