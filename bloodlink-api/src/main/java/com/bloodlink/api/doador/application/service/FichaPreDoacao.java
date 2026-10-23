package com.bloodlink.api.doador.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record FichaPreDoacao(UUID doadorId, boolean bemDeSaude, boolean dormiu6h, boolean alimentadoHidratado,
                             boolean alcool12h, boolean procedimentoRecente, boolean gestanteAmamentando,
                             boolean doencaRelevante, boolean medicamentoOuVacina,
                             String observacoes, Instant atualizadaEm) {

    public static final int OBSERVACOES_MAXIMO = 1000;

    public FichaPreDoacao {
        if (doadorId == null || atualizadaEm == null) {
            throw new IllegalArgumentException("Dados obrigatórios da ficha ausentes");
        }
        if (observacoes != null) {
            observacoes = observacoes.strip();
            if (observacoes.isEmpty()) {
                observacoes = null;
            } else if (observacoes.length() > OBSERVACOES_MAXIMO) {
                throw new IllegalArgumentException(
                        "As observações devem ter no máximo " + OBSERVACOES_MAXIMO + " caracteres");
            }
        }
    }

    /** Respostas que pedem atenção da triagem. Não bloqueiam nada: quem decide é a equipe. */
    public List<String> alertas() {
        List<String> a = new ArrayList<>();
        if (!bemDeSaude) a.add("MAL_ESTAR_OU_INFECCAO");
        if (!dormiu6h) a.add("SONO_INSUFICIENTE");
        if (!alimentadoHidratado) a.add("NAO_ALIMENTADO_OU_HIDRATADO");
        if (alcool12h) a.add("ALCOOL_RECENTE");
        if (procedimentoRecente) a.add("PROCEDIMENTO_RECENTE");
        if (gestanteAmamentando) a.add("GESTANTE_OU_AMAMENTANDO");
        if (doencaRelevante) a.add("CONDICAO_DE_SAUDE");
        if (medicamentoOuVacina) a.add("MEDICAMENTO_OU_VACINA");
        return List.copyOf(a);
    }

    /** Dado de saúde: nada disto sai em log. */
    @Override
    public String toString() {
        return "FichaPreDoacao[doadorId=" + doadorId + "]";
    }
}
