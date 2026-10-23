package com.bloodlink.api.doador.domain;

public enum StatusElegibilidade {
    /** Pode doar hoje. */
    ELEGIVEL,
    /** Vai poder doar numa data futura (intervalo mínimo ou limite anual). */
    AGUARDANDO,
    /** Não pode doar (idade, peso ou impedimento registrado). */
    IMPEDIDO
}


