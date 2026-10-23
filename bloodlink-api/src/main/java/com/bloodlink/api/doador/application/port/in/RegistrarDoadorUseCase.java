package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.RegistrarDoadorCommand;
import com.bloodlink.api.doador.domain.Doacao;
import com.bloodlink.api.doador.domain.Doador;

import java.util.List;
import java.util.UUID;

public interface RegistrarDoadorUseCase {
    Doador registrar(RegistrarDoadorCommand comando);

    List<Doacao> historico(UUID doadorId);

}

