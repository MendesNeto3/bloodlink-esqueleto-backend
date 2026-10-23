package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.DoadorResumo;
import com.bloodlink.api.doador.application.port.out.FiltroDoador;
import com.bloodlink.api.shared.Pagina.Pagina;

public interface ListarDoadoresUseCase {
    Pagina<DoadorResumo> listar(FiltroDoador filtro, int pagina, int tamanho);
}
