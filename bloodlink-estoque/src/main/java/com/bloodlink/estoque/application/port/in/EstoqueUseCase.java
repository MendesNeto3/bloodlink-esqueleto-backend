package com.bloodlink.estoque.application.port.in;

import com.bloodlink.estoque.domain.Lote;
import com.bloodlink.estoque.domain.Movimentacao;
import com.bloodlink.estoque.domain.ResumoEstoque;
import com.bloodlink.estoque.domain.TipoSanguineo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EstoqueUseCase {

    ResumoEstoque resumo();

    List<Lote> listarLotes(String tipoSanguineo, boolean incluirVencidos);

    ResultadoEntrada registrarEntrada(EntradaLoteComando comando);

    ResultadoBaixa darBaixa(BaixaComando comando);

    void atualizarParametros(String tipoSanguineo, Integer limiarCritico, Integer metaUnidades);

    /** tipo, de e ate são opcionais; sem datas, traz os últimos 30 dias. */
    List<Movimentacao> listarMovimentacoes(String tipoSanguineo, LocalDate de, LocalDate ate);

    record EntradaLoteComando(String tipoSanguineo, Integer quantidadeUnidades, LocalDate dataColeta,
                              LocalDate dataValidade, String chaveIdempotencia, UUID registradoPor) {
    }

    /** criado = false quando a chave de idempotência já tinha sido usada para esta mesma entrada. */
    record ResultadoEntrada(Lote lote, boolean criado) {
    }

    record BaixaComando(String tipoSanguineo, Integer quantidadeUnidades, String motivo, UUID registradoPor) {
    }

    record ResultadoBaixa(TipoSanguineo tipoSanguineo, int unidadesRetiradas, long unidadesRestantes) {
    }
}