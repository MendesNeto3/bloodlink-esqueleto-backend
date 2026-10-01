package com.bloodlink.centro.usuario.application.port.out;

import com.bloodlink.centro.usuario.domain.Usuario;
import com.bloodlink.common.correlation.Papel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryPort {

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorId(UUID id);

    List<Usuario> listarPorCentro(UUID centroId);

    boolean existeUsuarioComPapel(Papel papel);

    Usuario salvar(Usuario usuario);

}
