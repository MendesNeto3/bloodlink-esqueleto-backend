package com.bloodlink.api.doador.adapter.in.web.dto;

import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.SexoBiologico;
import com.bloodlink.api.doador.domain.TipoSanguineo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DoadorResponse(
        UUID id,
        String nome,
        String cpfMascarado,
        LocalDate dataNascimento,
        SexoBiologico sexo,
        TipoSanguineo tipoSanguineo,
        BigDecimal pesoKg,
        String telefone,
        String email,
        boolean consentimentoResponsavel,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static DoadorResponse de(Doador d) {
        return new DoadorResponse(
                d.id(), d.nome(), mascarar(d.cpf()), d.dataNascimento(), d.sexo(),
                d.tipoSanguineo(), d.pesoKg(), d.telefone(), d.email(),
                d.consentimentoResponsavel(), d.ativo(), d.criadoEm(), d.atualizadoEm());
    }

    private static String mascarar(String cpf) {
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }
}
