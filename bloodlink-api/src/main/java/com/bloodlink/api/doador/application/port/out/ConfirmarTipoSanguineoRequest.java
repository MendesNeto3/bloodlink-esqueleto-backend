package com.bloodlink.api.doador.application.port.out;

import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;

public record ConfirmarTipoSanguineoRequest(String tipoSanguineo) {

    public TipoSanguineo paraTipo () {
        return TipoSanguineo.de(tipoSanguineo)
                .orElseThrow(() -> new RegraDeNegocioException("Tipo sanguíneo inválido"));
    }
}
