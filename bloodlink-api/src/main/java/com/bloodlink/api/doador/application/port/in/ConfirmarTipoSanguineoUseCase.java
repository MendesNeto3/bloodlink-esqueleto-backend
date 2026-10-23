package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.TipoSanguineo;

import java.util.UUID;

public interface ConfirmarTipoSanguineoUseCase {
    Doador confirmar(UUID doadorId, TipoSanguineo tipo);
}
