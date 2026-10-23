package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.application.port.out.Impedimento;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ImpedimentoRepositoryAdapter implements ImpedimentoRepositoryPort{

    private final ImpedimentoJpaRepository impedimentoJpaRepository;

    public ImpedimentoRepositoryAdapter (ImpedimentoJpaRepository impedimentoJpaRepository) {
        this.impedimentoJpaRepository = impedimentoJpaRepository;
    }

    @Override
    public Impedimento salvar(Impedimento impedimento) {
        return impedimentoJpaRepository.saveAndFlush(ImpedimentoJpaEntity.de(impedimento)).paraDominio();
    }

    @Override
    public Optional<Impedimento> buscarPorId(UUID id) {
        return impedimentoJpaRepository.findById(id)
                .map(ImpedimentoJpaEntity::paraDominio);
    }

    @Override
    public List<Impedimento> listarPorDoador(UUID doadorId) {
        return impedimentoJpaRepository.findByDoadorIdOrderByCriadoEmDesc(doadorId)
                .stream()
                .map(ImpedimentoJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public Map<UUID, List<Impedimento>> naoEncerradosPorDoadores(Collection<UUID> doadorIds) {
        if (doadorIds.isEmpty()) {
            return Map.of();
        }
        return impedimentoJpaRepository.findByDoadorIdInAndEncerradoEmIsNull(doadorIds).stream()
                .map(ImpedimentoJpaEntity::paraDominio)
                .collect(Collectors.groupingBy(Impedimento::doadorId));
    }
}
