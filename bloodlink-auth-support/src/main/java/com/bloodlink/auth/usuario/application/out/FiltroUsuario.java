package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.usuario.Papel;

public record FiltroUsuario(String email, Papel papel, String centroId, Boolean ativo) {}
