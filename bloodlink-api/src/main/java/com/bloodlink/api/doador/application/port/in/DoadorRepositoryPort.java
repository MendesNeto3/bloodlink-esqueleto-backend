package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.FiltroDoador;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.shared.Pagina.Pagina;

import java.util.Optional;
import java.util.UUID;

public interface DoadorRepositoryPort {

    Doador salvar (Doador doador);
    Optional<Doador> buscarPorId(UUID id);
    Optional<Doador> buscarPorCpf(String cpf);
    boolean existePorCpf (String cpf);
    Pagina<Doador> listar(FiltroDoador filtro, int Pagina, int tamanho);
}
