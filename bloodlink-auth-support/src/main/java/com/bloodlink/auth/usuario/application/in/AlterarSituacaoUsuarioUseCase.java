package com.bloodlink.auth.usuario.application.in;

import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;

import java.util.UUID;

public interface AlterarSituacaoUsuarioUseCase {
    Usuario inativar (UUID id, Solicitante solicitante);
    Usuario reativar (UUID id, Solicitante solicitante);
}
