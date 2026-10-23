package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.RegistrarDoacaoComando;
import com.bloodlink.api.doador.domain.Doacao;

import java.util.List;
import java.util.UUID;

public interface DoacaoUseCase {

    Doacao registrar (RegistrarDoacaoComando registrarDoadorCommand);

    List<Doacao> historico (UUID doadorId);
}
