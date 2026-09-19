package com.bloodlink.centro.usuario.application.port.out;

import com.bloodlink.centro.usuario.domain.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryPort {

    Optional<Usuario> buscarPorEmail(String email);

    Usuario salvar (Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    List<Usuario> usariosPorCentro(UUID centroId);
}
