package com.bloodlink.api.doador.adapter.in.web.dto;

import com.bloodlink.api.doador.application.port.out.AtualizarDoadorCommand;

import java.math.BigDecimal;

public record AtualizarDoadorRequest(
        String nome,
        String telefone,
        String email,
        BigDecimal pesoKg,
        Boolean consentimentoResponsavel
) {
    AtualizarDoadorCommand paraComando() {
        return new AtualizarDoadorCommand(nome, telefone, email, pesoKg, consentimentoResponsavel);
    }
}