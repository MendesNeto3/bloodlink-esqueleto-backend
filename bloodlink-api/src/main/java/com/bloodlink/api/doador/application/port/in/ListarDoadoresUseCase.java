package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.shared.Pagina.Pagina;

public interface ListarDoadoresUseCase {
    Pagina<Doador> listar(String nome, String tipoSanguineo, Boolean ativo, int pagina, int tamanho);
}
