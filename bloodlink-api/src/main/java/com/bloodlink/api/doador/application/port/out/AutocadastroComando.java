package com.bloodlink.api.doador.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados do autocadastro feito pelo próprio doador, já sem formatação de tela. */
public record AutocadastroComando(
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
        RespostasFicha ficha) {

    public record RespostasFicha(boolean bemDeSaude, boolean dormiu6h, boolean alimentadoHidratado,
                                 boolean alcool12h, boolean procedimentoRecente, boolean gestanteAmamentando,
                                 boolean doencaRelevante, boolean medicamentoOuVacina, String observacoes) {
        /** Respostas de saúde são dado sensível: nunca vão para log. */
        @Override
        public String toString() {
            return "RespostasFicha[***]";
        }
    }

    /** Nome, CPF e e-mail nunca saem em log. */
    @Override
    public String toString() {
        return "AutocadastroComando[centroId=" + centroId + "]";
    }
}
