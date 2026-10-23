package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.application.port.out.Impedimento;

import java.util.*;

public interface ImpedimentoRepositoryPort {

    Impedimento salvar(Impedimento impedimento);

    Optional<Impedimento> buscarPorId(UUID id);

    /** Todos os impedimentos do doador, inclusive os encerrados, do mais recente para o mais antigo. */
    List<Impedimento> listarPorDoador(UUID doadorId);

    /** Impedimentos não encerrados de vários doadores em uma consulta só. */
    Map<UUID, List<Impedimento>> naoEncerradosPorDoadores(Collection<UUID> doadorIds);
}
