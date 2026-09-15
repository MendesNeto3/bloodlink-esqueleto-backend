package com.bloodlink.centro.usuario.application.port.in;

import java.util.Optional;

public interface AutenticarUsuarioUseCase {

    Optional<String> autenticar (String email, String senha);

}
