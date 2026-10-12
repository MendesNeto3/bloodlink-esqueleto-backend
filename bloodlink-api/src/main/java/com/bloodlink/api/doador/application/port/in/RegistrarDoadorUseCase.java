package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.RegistrarDoadorCommand;
import com.bloodlink.api.doador.domain.Doador;

public interface RegistrarDoadorUseCase {
    Doador registrar(RegistrarDoadorCommand comando);
}

