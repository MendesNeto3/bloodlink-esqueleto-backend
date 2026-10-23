package com.bloodlink.auth.usuario.application.in;

import com.bloodlink.auth.usuario.Solicitante;

public interface AlterarSenhaUseCase {
    void alterar (AlterarSenhaCommand comando, Solicitante solicitante);
}
