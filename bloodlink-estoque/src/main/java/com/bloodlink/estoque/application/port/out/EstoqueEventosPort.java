package com.bloodlink.estoque.application.port.out;

import com.bloodlink.estoque.domain.TipoSanguineo;

public interface EstoqueEventosPort {

    /** Avisa que o tipo ficou crítico. Não pode falhar a operação que o chamou. */
    void estoqueCritico(TipoSanguineo tipo, long unidades, int limiarCritico);
}