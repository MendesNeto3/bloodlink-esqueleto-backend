package com.bloodlink.auth.usuario.application.in.web;

import java.util.UUID;

public record CriarContaDoadorCommand(String nome, String email, String centroId, UUID doadorId) {
    @Override
    public String toString() {
        return "CriarContaDoadorCommand[doadorId=" + doadorId + "]";
    }
}

