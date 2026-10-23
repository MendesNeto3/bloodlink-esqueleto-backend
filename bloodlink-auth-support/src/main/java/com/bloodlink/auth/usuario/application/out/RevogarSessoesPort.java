package com.bloodlink.auth.usuario.application.out;

import java.util.UUID;

public interface RevogarSessoesPort {
    void revogarTodasDoUsuario(UUID id);
}
