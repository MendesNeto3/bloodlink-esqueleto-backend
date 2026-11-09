package com.bloodlink.estoque.application.port.in;

import com.bloodlink.estoque.domain.Lote;
import com.bloodlink.estoque.domain.ResumoEstoque;
import com.bloodlink.estoque.domain.TipoSanguineo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EstoqueUseCase {

    ResumoEstoque resumo();

    List<Lote> listarLotes(String tipoSanguineo, boolean incluirVencidos);

    Lote registrarEntrada(EntradaLoteComando comando);

    ResultadoBaixa darBaixa(BaixaComando comando);

    void atualizarParametros(String tipoSanguineo, Integer limiarCritico, Integer metaUnidades);

    record EntradaLoteComando(String tipoSanguineo, Integer quantidadeUnidades, LocalDate dataColeta,
                              LocalDate dataValidade, UUID registradoPor) {
    }

    record BaixaComando(String tipoSanguineo, Integer quantidadeUnidades, UUID registradoPor) {
    }

    record ResultadoBaixa(TipoSanguineo tipoSanguineo, int unidadesRetiradas, long unidadesRestantes) {
    }
}