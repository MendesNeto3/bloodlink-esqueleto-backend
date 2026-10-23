package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.usuario.Usuario;

public interface EmissorTokenPort {
    String emitirAccessToken(Usuario usuario);

    long validadeAccessSegundos();
}

