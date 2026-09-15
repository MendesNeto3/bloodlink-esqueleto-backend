package com.bloodlink.centro.usuario.application.port.in;

import com.bloodlink.centro.usuario.domain.Usuario;
import com.bloodlink.common.correlation.Papel;

import java.util.UUID;

public interface CadastrarUsuarioUseCase {
    Usuario cadastrar (String email, String senha, Papel papel, UUID centroId);

}
