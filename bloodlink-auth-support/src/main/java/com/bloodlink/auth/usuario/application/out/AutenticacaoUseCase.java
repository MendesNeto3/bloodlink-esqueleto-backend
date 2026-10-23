package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.usuario.application.in.web.LoginCommand;

public interface AutenticacaoUseCase {
    TokensEmitidos login(LoginCommand comando);

    void logout(String refreshToken);


}
