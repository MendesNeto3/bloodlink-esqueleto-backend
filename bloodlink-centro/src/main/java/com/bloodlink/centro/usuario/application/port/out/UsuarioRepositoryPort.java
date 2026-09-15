package com.bloodlink.centro.usuario.application.port.out;

import com.bloodlink.centro.usuario.domain.Usuario;

import java.util.Optional;

public interface UsuarioRepositoryPort {

    Optional<Usuario> buscarPorEmail(String email);

    Usuario salvar (Usuario usuario);

}
