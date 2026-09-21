package com.bloodlink.centro.application.port.out;

import com.bloodlink.centro.domain.Centro;
import com.bloodlink.centro.usuario.domain.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CentroRepositoryPort {

    List<Centro> listarTodos();

    Optional<Centro> buscarPorId(UUID id);

    Centro salvar (Centro centro);
}
