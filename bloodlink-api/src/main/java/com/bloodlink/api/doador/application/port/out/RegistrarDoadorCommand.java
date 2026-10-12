package com.bloodlink.api.doador.application.port.out;


import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarDoadorCommand
        (String nome,
         String cpf,
         LocalDate dataNascimento,
         String sexo,
         String tipoSanguineo,
         BigDecimal pesoKg,
         String telefone,
         String email,
         Boolean consentimentoResponsavel) {}