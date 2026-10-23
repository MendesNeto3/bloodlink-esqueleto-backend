package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;

final class EscopoUsuario {

    private EscopoUsuario() { }

    /**
     * Quem pode ver/alterar quem: a plataforma vê todos; o admin do centro, só o seu centro;
     * recepção, enfermeiro e doador enxergam apenas a si mesmos.
     */
    static boolean enxerga(Solicitante solicitante, Usuario alvo) {
        return switch (solicitante.papel()) {
            case ADMIN_PLATAFORMA -> true;
            case ADMIN_CENTRO -> solicitante.centroId() != null && solicitante.centroId().equals(alvo.centroId());
            case RECEPCAO, ENFERMEIRO, DOADOR -> solicitante.id().equals(alvo.id());
        };
    }
}