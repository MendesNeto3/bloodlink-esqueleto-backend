package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.domain.Doador;

import java.util.UUID;

public interface ConsultarDoadorUseCase {
    Doador porId(UUID id);

    Doador porCpf(String cpf);
}
