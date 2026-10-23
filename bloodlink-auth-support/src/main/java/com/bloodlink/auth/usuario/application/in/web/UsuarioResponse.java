package com.bloodlink.auth.usuario.application.in.web;

import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Usuario;

import java.time.Instant;
import java.util.UUID;

public record UsuarioResponse(UUID id, String nome, String email, Papel papel, String centroId,
                              boolean ativo, Instant criadoEm, Instant atualizadoEm) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.id(), u.nome(), u.email(), u.papel(), u.centroId(),
                u.ativo(), u.criadoEm(), u.atualizadoEm());
    }

}
