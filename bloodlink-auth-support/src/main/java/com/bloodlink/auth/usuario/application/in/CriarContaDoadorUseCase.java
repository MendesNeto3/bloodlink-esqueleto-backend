package com.bloodlink.auth.usuario.application.in;

import com.bloodlink.auth.usuario.application.in.web.CriarContaDoadorCommand;

import java.util.UUID;

public interface CriarContaDoadorUseCase {
    UUID criar(CriarContaDoadorCommand comando);
}
