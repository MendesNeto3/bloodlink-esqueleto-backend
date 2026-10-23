package com.bloodlink.api.doador.doadorException;

import java.util.UUID;

public class AtualizacaoDoadorException extends RuntimeException {
    public AtualizacaoDoadorException(UUID doadorId, Throwable causa) {
        super("Falha ao atualizar o doador", causa);
    }
}
