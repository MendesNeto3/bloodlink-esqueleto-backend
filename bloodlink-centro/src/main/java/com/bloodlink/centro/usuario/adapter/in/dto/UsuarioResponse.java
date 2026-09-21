package com.bloodlink.centro.usuario.adapter.in.dto;

import com.bloodlink.centro.usuario.domain.Usuario;
import com.bloodlink.common.correlation.Papel;

import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String email,
        Papel papel,
        UUID centroId
) {
    public static UsuarioResponse to(Usuario usuario) {
        return new UsuarioResponse(usuario.id(), usuario.email(), usuario.papel(), usuario.centroId());
    }
}