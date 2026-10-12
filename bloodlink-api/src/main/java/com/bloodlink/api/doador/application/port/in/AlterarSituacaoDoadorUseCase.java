package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.domain.Doador;

import java.util.UUID;

public interface AlterarSituacaoDoadorUseCase {
    Doador inativar (UUID id);
    Doador reativar (UUID id);
}
