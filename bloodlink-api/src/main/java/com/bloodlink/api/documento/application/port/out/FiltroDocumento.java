package com.bloodlink.api.documento.application.port.out;

import com.bloodlink.api.documento.domain.CategoriaDocumento;

import java.util.UUID;

/** Todos os campos são opcionais. "busca" procura no nome do arquivo e no nome do doador. */
public record FiltroDocumento(CategoriaDocumento categoria, UUID doadorId, String busca) {
}
