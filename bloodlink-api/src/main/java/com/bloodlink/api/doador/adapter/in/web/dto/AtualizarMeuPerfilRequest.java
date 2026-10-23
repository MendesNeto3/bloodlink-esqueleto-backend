package com.bloodlink.api.doador.adapter.in.web.dto;

import com.bloodlink.api.doador.application.port.out.AtualizarPerfilDoadorCommand;

import java.math.BigDecimal;
import java.util.UUID;

public record AtualizarMeuPerfilRequest(String telefone, String cidade, String uf, BigDecimal pesoKg) {

    public AtualizarPerfilDoadorCommand paraComando(UUID doadorId) {
        return new AtualizarPerfilDoadorCommand(doadorId, telefone, cidade, uf, pesoKg);
    }

    @Override
    public String toString() {
        return "AtualizarMeuPerfilRequest[...]";
    }
}
