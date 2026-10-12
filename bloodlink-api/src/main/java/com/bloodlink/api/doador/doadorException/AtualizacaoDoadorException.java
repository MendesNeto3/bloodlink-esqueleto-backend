package com.bloodlink.api.doador.doadorException;

import java.util.UUID;

public class AtualizacaoDoadorException extends RuntimeException {
    private final UUID doadorId;

    public AtualizacaoDoadorException(UUID doadorId, Throwable causa) {
        super("Falha ao atualizar o doador", causa);
        this.doadorId = doadorId;
    }

    public UUID getDoadorId() {
        return doadorId;
    }
}
