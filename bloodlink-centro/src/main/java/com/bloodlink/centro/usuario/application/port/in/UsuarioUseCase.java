package com.bloodlink.centro.usuario.application.port.in;

import com.bloodlink.centro.usuario.domain.Usuario;
import com.bloodlink.common.correlation.Papel;

import java.util.List;
import java.util.UUID;

public interface UsuarioUseCase {
    Usuario cadastrar (String email, String senha, Papel papel, UUID centroId);

    Usuario atualizarPapel(UUID usuarioId, Papel novoPapel);

    List<Usuario> listarPorCentro(UUID centroId);

}
