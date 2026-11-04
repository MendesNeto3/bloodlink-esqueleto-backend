package com.bloodlink.api.doador.application.port.in;

import java.util.UUID;

public interface EnviarConviteUseCase {
    void enviar(UUID doadorId, String centroId);
}
