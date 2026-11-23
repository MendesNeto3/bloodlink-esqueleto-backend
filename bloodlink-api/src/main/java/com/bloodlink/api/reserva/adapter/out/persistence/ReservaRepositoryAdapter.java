package com.bloodlink.api.reserva.adapter.out.persistence;

import com.bloodlink.api.reserva.application.port.out.ReservaRepositoryPort;
import com.bloodlink.api.reserva.domain.Reserva;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReservaRepositoryAdapter implements ReservaRepositoryPort {

    private final ReservaJpaRepository jpa;

    public ReservaRepositoryAdapter(ReservaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Reserva salvar(Reserva reserva) {
        return jpa.save(ReservaJpaEntity.de(reserva)).paraDominio();
    }

    @Override
    public Optional<Reserva> buscarPorId(UUID id) {
        return jpa.findById(id).map(ReservaJpaEntity::paraDominio);
    }

    @Override
    public List<Reserva> listarRecentes(int limite) {
        return jpa.findTop500ByOrderByCriadoEmDesc().stream()
                .limit(limite)
                .map(ReservaJpaEntity::paraDominio)
                .toList();
    }
}
