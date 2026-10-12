package com.bloodlink.api.doador.application.port.out;

import com.bloodlink.api.doador.domain.TipoSanguineo;

public record FiltroDoador(String nome, TipoSanguineo tipoSanguineo, Boolean ativo) {
}

