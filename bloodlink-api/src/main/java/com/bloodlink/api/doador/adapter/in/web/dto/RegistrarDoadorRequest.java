package com.bloodlink.api.doador.adapter.in.web.dto;

import com.bloodlink.api.doador.application.port.out.RegistrarDoadorCommand;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarDoadorRequest(
        String nome,
        String cpf,
        LocalDate dataNascimento,
        String sexo,
        String tipoSanguineo,
        BigDecimal pesoKg,
        String telefone,
        String email,
        Boolean consentimentoResponsavel
) {
    public RegistrarDoadorCommand paraComando() {
        return new RegistrarDoadorCommand(nome, cpf, dataNascimento, sexo, tipoSanguineo,
                pesoKg, telefone, email, consentimentoResponsavel);
    }
}
