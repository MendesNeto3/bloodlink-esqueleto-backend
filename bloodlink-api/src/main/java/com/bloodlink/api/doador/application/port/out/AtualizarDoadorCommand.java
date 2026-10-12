package com.bloodlink.api.doador.application.port.out;

import java.math.BigDecimal;

public record AtualizarDoadorCommand(String nome, String telefone, String email, BigDecimal pesoKg,
                                     Boolean consentimentoResponsavel) {}
