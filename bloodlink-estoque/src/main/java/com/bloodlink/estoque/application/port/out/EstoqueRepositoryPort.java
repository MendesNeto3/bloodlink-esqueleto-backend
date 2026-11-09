package com.bloodlink.estoque.application.port.out;

import com.bloodlink.estoque.domain.Lote;
import com.bloodlink.estoque.domain.SituacaoTipo;
import com.bloodlink.estoque.domain.TipoSanguineo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EstoqueRepositoryPort {

    List<SituacaoTipo> situacaoPorTipo(LocalDate hoje, LocalDate vencendoAte);

    /** tipo e validadeMinima são filtros opcionais (null = sem filtro); só traz lotes com saldo. */
    List<Lote> listarLotes(TipoSanguineo tipo, LocalDate validadeMinima);

    void salvarLote(Lote lote);

    /** Lotes válidos do tipo, do que vence primeiro ao que vence depois, já travados para a baixa. */
    List<LoteDisponivel> bloquearLotesParaBaixa(TipoSanguineo tipo, LocalDate hoje);

    void atualizarQuantidadeLote(UUID id, int quantidade);

    boolean atualizarParametros(TipoSanguineo tipo, int limiarCritico, int metaUnidades);

    record LoteDisponivel(UUID id, int quantidade) {
    }
}