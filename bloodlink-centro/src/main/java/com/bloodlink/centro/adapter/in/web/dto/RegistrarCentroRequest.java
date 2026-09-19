package com.bloodlink.centro.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistrarCentroRequest(@NotBlank String nome) {
}
