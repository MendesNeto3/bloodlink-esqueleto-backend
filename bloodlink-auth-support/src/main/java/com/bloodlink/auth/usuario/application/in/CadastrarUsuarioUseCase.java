package com.bloodlink.auth.usuario.application.in;

import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;

public interface CadastrarUsuarioUseCase {
    Usuario cadastrar(CadastrarUsuarioCommand comando, Solicitante solicitante);
}
