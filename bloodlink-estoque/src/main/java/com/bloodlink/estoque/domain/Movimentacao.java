package com.bloodlink.estoque.domain;

import java.time.Instant;
import java.util.UUID;

public record Movimentacao(UUID id, TipoSanguineo tipoSanguineo, UUID loteId, TipoMovimentacao tipo,
                           int quantidade, MotivoBaixa motivo, UUID registradoPor,
                           String chaveIdempotencia, Instant criadoEm) {

    public static Movimentacao entrada(Lote lote, String chaveIdempotencia) {
        return new Movimentacao(UUID.randomUUID(), lote.tipoSanguineo(), lote.id(), TipoMovimentacao.ENTRADA,
                lote.quantidadeUnidades(), null, lote.registradoPor(), chaveIdempotencia, null);
    }

    public static Movimentacao baixa(TipoSanguineo tipo, UUID loteId, int quantidade, MotivoBaixa motivo,
                                     UUID registradoPor) {
        return new Movimentacao(UUID.randomUUID(), tipo, loteId, TipoMovimentacao.BAIXA,
                quantidade, motivo, registradoPor, null, null);
    }
}
