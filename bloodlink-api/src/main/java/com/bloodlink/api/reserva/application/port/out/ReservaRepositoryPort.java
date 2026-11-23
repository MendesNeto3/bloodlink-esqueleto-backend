package com.bloodlink.api.reserva.application.port.out;

import com.bloodlink.api.reserva.domain.Reserva;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservaRepositoryPort {
    Reserva salvar(Reserva reserva);

    Optional<Reserva> buscarPorId(UUID id);

    /** Mais recentes primeiro, no máximo {@code limite}. */
    List<Reserva> listarRecentes(int limite);
}
