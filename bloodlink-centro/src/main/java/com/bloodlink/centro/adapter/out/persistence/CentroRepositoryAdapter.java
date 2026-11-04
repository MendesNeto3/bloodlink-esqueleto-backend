package com.bloodlink.centro.adapter.out.persistence;

import com.bloodlink.centro.application.port.out.CentroRepositoryPort;
import com.bloodlink.centro.domain.Centro;
import com.bloodlink.centro.domain.DiaSemana;
import com.bloodlink.centro.domain.PerfilCentro;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CentroRepositoryAdapter implements CentroRepositoryPort {
    private final CentroJpaRepository centroJpaRepository;

    public CentroRepositoryAdapter(CentroJpaRepository centroJpaRepository) {
        this.centroJpaRepository = centroJpaRepository;
    }

    @Override
    public List<Centro> listarTodos() {
        return centroJpaRepository
                .findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Centro> buscarPorId(UUID id) {
        return centroJpaRepository
                .findById(id)
                .map(this::toDomain);
    }

    @Override
    public Centro salvar(Centro centro) {
        CentroJpaEntity entity = new CentroJpaEntity();
        entity.setId(centro.id() != null ? centro.id() : UUID.randomUUID());
        entity.setNome(centro.nome());
        entity.setSchema(centro.schema());
        entity.setAtivo(centro.ativo());

        PerfilCentro perfil = centro.perfil();
        entity.setTipo(perfil.tipo());
        entity.setEndereco(perfil.endereco());
        entity.setCidade(perfil.cidade());
        entity.setUf(perfil.uf());
        entity.setLatitude(perfil.latitude());
        entity.setLongitude(perfil.longitude());
        entity.setDiasFuncionamento(perfil.diasFuncionamento().isEmpty() ? null
                : perfil.diasFuncionamento().stream().map(DiaSemana::name).collect(Collectors.joining(",")));
        entity.setAbertura(perfil.abertura());
        entity.setFechamento(perfil.fechamento());

        return toDomain(centroJpaRepository.save(entity));
    }

    private Centro toDomain(CentroJpaEntity entity) {
        Set<DiaSemana> dias = (entity.getDiasFuncionamento() == null || entity.getDiasFuncionamento().isBlank())
                ? Set.of()
                : Arrays.stream(entity.getDiasFuncionamento().split(","))
                .map(DiaSemana::valueOf)
                .collect(Collectors.toSet());
        PerfilCentro perfil = new PerfilCentro(entity.getTipo(), entity.getEndereco(), entity.getCidade(),
                entity.getUf(), entity.getLatitude(), entity.getLongitude(), dias,
                entity.getAbertura(), entity.getFechamento());
        return new Centro(entity.getId(), entity.getNome(), entity.getSchema(), entity.isAtivo(), perfil);
    }
}