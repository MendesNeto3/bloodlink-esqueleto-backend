package com.bloodlink.centro.application.port.in;

import com.bloodlink.centro.domain.Centro;
import com.bloodlink.centro.domain.CentroComDistancia;
import com.bloodlink.centro.domain.PerfilCentro;

import java.util.List;
import java.util.UUID;

public interface CentroUseCase {
    Centro registrar(String nome);

    List<Centro> listar();

    List<Centro> listarAtivos();

    List<CentroComDistancia> listarPublicos(Double lat, Double lng);

    /** centroIdSolicitante null = ADMIN_PLATAFORMA (pode tudo); senão só o próprio centro. */
    Centro atualizarNome(UUID id, String nome, String centroIdSolicitante);

    Centro atualizarPerfil(UUID id, PerfilCentro perfil, String centroIdSolicitante);

    Centro desativar(UUID id);
}