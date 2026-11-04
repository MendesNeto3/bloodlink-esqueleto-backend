package com.bloodlink.api.agendamento.domain;

public enum StatusAgendamento {
    PENDENTE, CONFIRMADO, CONCLUIDO, CANCELADO, FALTOU;

    public boolean ativo () {
        return this == PENDENTE || this == CONFIRMADO;
    }
}
