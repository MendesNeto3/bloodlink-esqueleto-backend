package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.ResumoDoador;

import java.util.UUID;

public interface ObterResumoDoadorUseCase {

    ResumoDoador obter(UUID doadorId);

}
