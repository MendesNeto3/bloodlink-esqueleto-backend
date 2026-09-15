package com.bloodlink.centro.usuario.adapter.in.dto;

import com.bloodlink.common.correlation.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CadastrarUsuarioRequest (
        @NotBlank @Email String email,
        @NotBlank String senha,
        @NotNull Papel papel,
        @NotNull UUID centroId
) {
}