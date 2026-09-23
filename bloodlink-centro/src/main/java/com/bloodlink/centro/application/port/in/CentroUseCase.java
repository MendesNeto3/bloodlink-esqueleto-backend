package com.bloodlink.centro.application.port.in;

import com.bloodlink.centro.domain.Centro;

import java.util.List;
import java.util.UUID;

public interface CentroUseCase {
    Centro registrar(String nome);

    List<Centro> listar();

    Centro atualizarNome (UUID id, String nome);

    Centro desativar (UUID id);

}
