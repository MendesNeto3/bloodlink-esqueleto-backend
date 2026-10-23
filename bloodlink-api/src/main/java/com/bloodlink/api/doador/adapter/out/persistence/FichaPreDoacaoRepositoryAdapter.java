package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.application.service.FichaPreDoacao;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class FichaPreDoacaoRepositoryAdapter implements FichaPreDoacaoRepositoryPort {

    private final FichaPreDoacaoJpaRepository repositorio;

    public FichaPreDoacaoRepositoryAdapter(FichaPreDoacaoJpaRepository repositorio) {
        this.repositorio = repositorio;
    }


    @Override
    public FichaPreDoacao salvar(FichaPreDoacao ficha) {
        return repositorio.saveAndFlush(FichaPreDoacaoJpaEntity.de(ficha)).paraDominio();
    }

    @Override
    public Optional<FichaPreDoacao> buscarPorDoador(UUID doadorId) {
        return repositorio.findById(doadorId).map(FichaPreDoacaoJpaEntity::paraDominio);
    }
}