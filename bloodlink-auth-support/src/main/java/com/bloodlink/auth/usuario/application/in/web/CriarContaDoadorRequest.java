package com.bloodlink.auth.usuario.application.in.web;

import java.util.UUID;

public record CriarContaDoadorRequest(String nome, String email, String centroId, UUID doadorId) {
    public CriarContaDoadorCommand paraComando() {
        return new CriarContaDoadorCommand(nome, email, centroId, doadorId);
    }
}
