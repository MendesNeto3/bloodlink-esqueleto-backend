package com.bloodlink.centro.adapter.out.persistence;

import com.bloodlink.centro.application.port.out.CentroRepositoryPort;
import com.bloodlink.centro.domain.Centro;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CentroRepositoryAdapter implements CentroRepositoryPort {

    private final CentroJpaRepository centroJpaRepository;

    public CentroRepositoryAdapter(CentroJpaRepository centroJpaRepository) {
        this.centroJpaRepository = centroJpaRepository;
    }

    @Override
    public List<Centro> listarTodos() {
        return centroJpaRepository.
                findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Centro> buscarPorId(UUID id) {
        return centroJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Centro salvar(Centro centro) {
        CentroJpaEntity centroJpaEntity = new CentroJpaEntity();
        centroJpaEntity.setId(UUID.randomUUID());
        centroJpaEntity.setNome(centro.nome());
        centroJpaEntity.setSchema(centro.schema());
        centroJpaEntity.setAtivo(centro.ativo());

        CentroJpaEntity centroSave = centroJpaRepository.save(centroJpaEntity);

        return toDomain(centroSave);
    }

    private Centro toDomain(CentroJpaEntity entity) {
        return new Centro(entity.getId(), entity.getNome(), entity.getSchema(), entity.isAtivo());
    }
}
