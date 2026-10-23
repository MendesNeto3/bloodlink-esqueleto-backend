package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.Impedimento;
import com.bloodlink.api.doador.application.port.out.RegistrarImpedimentoComando;

import java.util.List;
import java.util.UUID;

public interface ImpedimentoUseCase {
    Impedimento registrar(RegistrarImpedimentoComando comando);

    /** Todos os impedimentos do doador, inclusive os encerrados, do mais recente para o mais antigo. */
    List<Impedimento> listar(UUID doadorId);

    Impedimento encerrar(UUID doadorId, UUID impedimentoId);
}
