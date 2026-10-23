package com.bloodlink.auth.usuario.application.in;

import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;

import java.util.UUID;

public interface ConsultarUsuarioUseCase {
    Usuario porId(UUID id, Solicitante solicitante);
    Usuario porEmail(String email, Solicitante solicitante);
    Usuario porNome(String nome, Solicitante solicitante);
}
