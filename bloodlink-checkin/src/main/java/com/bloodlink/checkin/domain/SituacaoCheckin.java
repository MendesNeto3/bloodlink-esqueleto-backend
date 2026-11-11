package com.bloodlink.checkin.domain;

import java.util.Locale;

public enum SituacaoCheckin {
    NAO_CHEGOU, AGUARDANDO_TRIAGEM, APTO, INAPTO, CONCLUIDO, FALTOU, CANCELADO;

    public static SituacaoCheckin de(String statusAgendamento, boolean temCheckin, ResultadoTriagem triagem) {
        if ("CONCLUIDO".equals(statusAgendamento)) {
            return CONCLUIDO;
        }
        if ("FALTOU".equals(statusAgendamento)) {
            return FALTOU;
        }
        if (triagem == ResultadoTriagem.INAPTA) {
            return INAPTO; // inapto vale mais que cancelado: o motivo do cancelamento foi a triagem
        }
        if ("CANCELADO".equals(statusAgendamento)) {
            return CANCELADO;
        }
        if (!temCheckin) {
            return NAO_CHEGOU;
        }
        return triagem == ResultadoTriagem.APTA ? APTO : AGUARDANDO_TRIAGEM;
    }

    public static SituacaoCheckin doFiltro(String valor) {
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Situação inválida: use NAO_CHEGOU, AGUARDANDO_TRIAGEM, APTO, "
                    + "INAPTO, CONCLUIDO, FALTOU ou CANCELADO");
        }
    }
}

