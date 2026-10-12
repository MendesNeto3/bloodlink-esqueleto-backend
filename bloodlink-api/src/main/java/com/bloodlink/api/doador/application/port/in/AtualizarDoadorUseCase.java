package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.AtualizarDoadorCommand;
import com.bloodlink.api.doador.domain.Doador;

import java.util.UUID;

public interface AtualizarDoadorUseCase {
    Doador atualizar(UUID id, AtualizarDoadorCommand comando);
}
