package com.bloodlink.auth.usuario.application.out;

public interface RenovarTokenUseCase {

    TokensEmitidos renovar(String refreshToken);

}
