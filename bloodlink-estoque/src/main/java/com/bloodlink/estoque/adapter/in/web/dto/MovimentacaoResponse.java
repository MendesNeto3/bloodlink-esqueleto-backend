package com.bloodlink.estoque.adapter.in.web.dto;

import com.bloodlink.estoque.domain.Movimentacao;

import java.time.Instant;
import java.util.UUID;

public record MovimentacaoResponse(UUID id, String tipoSanguineo, String rotulo, UUID loteId, String tipo,
                                   int quantidadeUnidades, String motivo, UUID registradoPor, Instant criadoEm) {

    public static MovimentacaoResponse de(Movimentacao m) {
        return new MovimentacaoResponse(m.id(), m.tipoSanguineo().name(), m.tipoSanguineo().rotulo(), m.loteId(),
                m.tipo().name(), m.quantidade(), m.motivo() == null ? null : m.motivo().name(),
                m.registradoPor(), m.criadoEm());
    }
}
