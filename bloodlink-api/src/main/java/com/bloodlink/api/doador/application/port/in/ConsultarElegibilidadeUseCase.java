package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.ResultadoElegibilidade;

import java.util.UUID;

public interface ConsultarElegibilidadeUseCase {
    ResultadoElegibilidade consultar(UUID doadorId);

}
