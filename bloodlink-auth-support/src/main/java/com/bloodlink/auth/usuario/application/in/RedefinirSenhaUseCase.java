package com.bloodlink.auth.usuario.application.in;

import com.bloodlink.auth.usuario.application.in.web.RedefinirSenhaCommand;

public interface RedefinirSenhaUseCase {

    /** Define a nova senha com o token recebido por e-mail; derruba as sessões abertas da conta. */
    void redefinir(RedefinirSenhaCommand comando);
}
