package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.SalvarFichaComando;
import com.bloodlink.api.doador.application.service.FichaPreDoacao;

import java.util.UUID;

public interface FichaPreDoacaoUseCase {

    FichaPreDoacao salvar(SalvarFichaComando comando);

    FichaPreDoacao consultar(UUID doadorId);
}
