package com.bloodlink.api.doador.adapter.in.web.dto;

import com.bloodlink.api.doador.application.port.out.AutocadastroComando;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AutocadastroDoadorRequest(
        String centroId,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        String sexo,
        String tipoSanguineo,
        BigDecimal pesoKg,
        String telefone,
        String email,
        String cidade,
        String uf,
        LocalDate ultimaDoacao,
        Boolean consentimentoResponsavel,
        Boolean aceiteArmazenamento,
        FichaPreDoacaoRequest ficha) {

    public AutocadastroComando paraComando() {
        if (ficha == null || ficha.bemDeSaude() == null || ficha.dormiu6h() == null
                || ficha.alimentadoHidratado() == null || ficha.alcool12h() == null
                || ficha.procedimentoRecente() == null || ficha.gestanteAmamentando() == null
                || ficha.doencaRelevante() == null || ficha.medicamentoOuVacina() == null) {
            throw new IllegalArgumentException("Responda todas as perguntas da ficha");
        }
        AutocadastroComando.RespostasFicha respostas = new AutocadastroComando.RespostasFicha(
                ficha.bemDeSaude(), ficha.dormiu6h(), ficha.alimentadoHidratado(), ficha.alcool12h(),
                ficha.procedimentoRecente(), ficha.gestanteAmamentando(), ficha.doencaRelevante(),
                ficha.medicamentoOuVacina(), ficha.observacoes());
        return new AutocadastroComando(centroId, nome, cpf, dataNascimento, sexo, tipoSanguineo, pesoKg,
                telefone, email, cidade, uf, ultimaDoacao, consentimentoResponsavel, aceiteArmazenamento,
                respostas);
    }
}
