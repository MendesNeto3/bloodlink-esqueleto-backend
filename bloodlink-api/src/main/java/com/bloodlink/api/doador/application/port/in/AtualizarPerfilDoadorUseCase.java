package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.AtualizarPerfilDoadorCommand;
import com.bloodlink.api.doador.domain.Doador;

public interface AtualizarPerfilDoadorUseCase {
    Doador atualizar(AtualizarPerfilDoadorCommand comando);
}
