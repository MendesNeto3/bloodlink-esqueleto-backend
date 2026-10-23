package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.application.service.FichaPreDoacao;

import java.util.Optional;
import java.util.UUID;

public interface FichaPreDoacaoRepositoryPort {
    FichaPreDoacao salvar(FichaPreDoacao ficha);

    Optional<FichaPreDoacao> buscarPorDoador(UUID doadorId);
}
