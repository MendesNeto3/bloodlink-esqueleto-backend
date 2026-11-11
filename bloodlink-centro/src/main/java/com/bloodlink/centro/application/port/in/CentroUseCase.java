package com.bloodlink.centro.application.port.in;

import com.bloodlink.centro.domain.Centro;
import com.bloodlink.centro.domain.CentroComDistancia;
import com.bloodlink.centro.domain.PerfilCentro;

import java.util.List;
import java.util.UUID;

public interface CentroUseCase {

    Centro registrar(String nome, PerfilCentro perfil);

    default Centro registrar(String nome) {
        return registrar(nome, PerfilCentro.vazio());
    }

    List<Centro> listar();

    List<Centro> listarAtivos();

    /** busca (opcional) procura no nome, na cidade e no endereço, sem diferenciar acento nem maiúscula. */
    List<CentroComDistancia> listarPublicos(Double lat, Double lng, String busca);

    default List<CentroComDistancia> listarPublicos(Double lat, Double lng) {
        return listarPublicos(lat, lng, null);
    }

    /** centroId = o que vem depois de "centro_" no nome do schema (12 caracteres hexadecimais). */
    Centro buscarPorCentroId(String centroId);

    /** centroIdSolicitante null = ADMIN_PLATAFORMA (pode tudo); senão só o próprio centro. */
    Centro atualizarNome(UUID id, String nome, String centroIdSolicitante);

    Centro atualizarPerfil(UUID id, PerfilCentro perfil, String centroIdSolicitante);

    Centro desativar(UUID id);
}