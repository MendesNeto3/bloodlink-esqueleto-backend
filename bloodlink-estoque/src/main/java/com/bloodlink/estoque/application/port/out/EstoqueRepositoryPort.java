package com.bloodlink.estoque.application.port.out;

import com.bloodlink.estoque.domain.Lote;
import com.bloodlink.estoque.domain.Movimentacao;
import com.bloodlink.estoque.domain.SituacaoTipo;
import com.bloodlink.estoque.domain.TipoSanguineo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstoqueRepositoryPort {

    List<SituacaoTipo> situacaoPorTipo(LocalDate hoje, LocalDate vencendoAte);

    /** tipo e validadeMinima são filtros opcionais (null = sem filtro); só traz lotes com saldo. */
    List<Lote> listarLotes(TipoSanguineo tipo, LocalDate validadeMinima);

    void salvarLote(Lote lote);

    Optional<Lote> buscarLote(UUID id);

    /** Lotes válidos do tipo, do que vence primeiro ao que vence depois, já travados para a baixa. */
    List<LoteDisponivel> bloquearLotesParaBaixa(TipoSanguineo tipo, LocalDate hoje);

    /** Para quantidade maior que zero; lote esgotado sai pelo removerLote. */
    void atualizarQuantidadeLote(UUID id, int quantidade);

    void removerLote(UUID id);

    boolean atualizarParametros(TipoSanguineo tipo, int limiarCritico, int metaUnidades);

    /** Lança RecursoNaoEncontradoException se o tipo não existir no estoque. */
    int limiarCritico(TipoSanguineo tipo);

    long saldoDisponivel(TipoSanguineo tipo, LocalDate hoje);

    /** false quando a chave de idempotência já existia (nada foi gravado). */
    boolean registrarMovimentacao(Movimentacao movimentacao);

    Optional<Movimentacao> buscarEntradaPorChave(String chaveIdempotencia);

    /** tipo é filtro opcional; do mais recente para o mais antigo; desde inclusivo, ate exclusivo. */
    List<Movimentacao> listarMovimentacoes(TipoSanguineo tipo, Instant desde, Instant ate, int limite);

    record LoteDisponivel(UUID id, int quantidade) {
    }
}