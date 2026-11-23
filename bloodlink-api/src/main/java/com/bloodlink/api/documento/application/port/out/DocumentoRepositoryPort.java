package com.bloodlink.api.documento.application.port.out;

import com.bloodlink.api.documento.domain.Documento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepositoryPort {
    Documento salvar(Documento documento);

    Optional<Documento> buscarPorId(UUID id);

    /** Mais recentes primeiro. */
    List<Documento> listar(FiltroDocumento filtro, int limite);

    long contarPorDoador(UUID doadorId);
}
