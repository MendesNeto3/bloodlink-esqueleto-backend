package com.bloodlink.estoque.adapter.out.persistence;

import com.bloodlink.estoque.application.exception.RecursoNaoEncontradoException;
import com.bloodlink.estoque.application.port.out.EstoqueRepositoryPort;
import com.bloodlink.estoque.domain.Lote;
import com.bloodlink.estoque.domain.MotivoBaixa;
import com.bloodlink.estoque.domain.Movimentacao;
import com.bloodlink.estoque.domain.SituacaoTipo;
import com.bloodlink.estoque.domain.TipoMovimentacao;
import com.bloodlink.estoque.domain.TipoSanguineo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EstoqueRepositoryAdapter implements EstoqueRepositoryPort {

    private static final String COLUNAS_MOVIMENTACAO = """
            id, tipo_sanguineo, lote_id, tipo_movimentacao, quantidade, motivo,
            registrado_por, chave_idempotencia, criado_em
            """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<SituacaoTipo> situacaoPorTipo(LocalDate hoje, LocalDate vencendoAte) {
        Query q = em.createNativeQuery("""
                select e.tipo_sanguineo,
                       coalesce(sum(case when l.data_validade >= :hoje then l.quantidade_unidades end), 0),
                       e.meta_unidades,
                       e.limiar_critico,
                       coalesce(sum(case when l.data_validade between :hoje and :limite then l.quantidade_unidades end), 0)
                  from estoque e left join lote_estoque l on l.tipo_sanguineo = e.tipo_sanguineo
                 group by e.tipo_sanguineo, e.meta_unidades, e.limiar_critico
                """);
        q.setParameter("hoje", hoje);
        q.setParameter("limite", vencendoAte);
        List<?> linhas = q.getResultList();
        return linhas.stream().map(l -> {
            Object[] c = (Object[]) l;
            return new SituacaoTipo(TipoSanguineo.valueOf((String) c[0]), ((Number) c[1]).longValue(),
                    ((Number) c[2]).intValue(), ((Number) c[3]).intValue(), ((Number) c[4]).longValue());
        }).toList();
    }

    @Override
    public List<Lote> listarLotes(TipoSanguineo tipo, LocalDate validadeMinima) {
        StringBuilder sql = new StringBuilder("""
                select id, tipo_sanguineo, quantidade_unidades, data_coleta, data_validade, registrado_por
                  from lote_estoque
                 where quantidade_unidades > 0
                """);
        if (tipo != null) {
            sql.append(" and tipo_sanguineo = :tipo");
        }
        if (validadeMinima != null) {
            sql.append(" and data_validade >= :minima");
        }
        sql.append(" order by data_validade, data_coleta, id");
        Query q = em.createNativeQuery(sql.toString());
        if (tipo != null) {
            q.setParameter("tipo", tipo.name());
        }
        if (validadeMinima != null) {
            q.setParameter("minima", validadeMinima);
        }
        List<?> linhas = q.getResultList();
        return linhas.stream().map(l -> lote((Object[]) l)).toList();
    }

    @Override
    public void salvarLote(Lote lote) {
        Query q = em.createNativeQuery("""
                insert into lote_estoque (id, tipo_sanguineo, quantidade_unidades, data_coleta, data_validade,
                                          registrado_por, criado_em)
                values (:id, :tipo, :quantidade, :coleta, :validade, :por, now())
                """);
        q.setParameter("id", lote.id());
        q.setParameter("tipo", lote.tipoSanguineo().name());
        q.setParameter("quantidade", lote.quantidadeUnidades());
        q.setParameter("coleta", lote.dataColeta());
        q.setParameter("validade", lote.dataValidade());
        q.setParameter("por", lote.registradoPor());
        q.executeUpdate();
    }

    @Override
    public Optional<Lote> buscarLote(UUID id) {
        Query q = em.createNativeQuery("""
                select id, tipo_sanguineo, quantidade_unidades, data_coleta, data_validade, registrado_por
                  from lote_estoque
                 where id = :id
                """);
        q.setParameter("id", id);
        List<?> linhas = q.getResultList();
        return linhas.stream().map(l -> lote((Object[]) l)).findFirst();
    }

    @Override
    public List<LoteDisponivel> bloquearLotesParaBaixa(TipoSanguineo tipo, LocalDate hoje) {
        Query q = em.createNativeQuery("""
                select id, quantidade_unidades
                  from lote_estoque
                 where tipo_sanguineo = :tipo and data_validade >= :hoje and quantidade_unidades > 0
                 order by data_validade, data_coleta, id
                   for update
                """);
        q.setParameter("tipo", tipo.name());
        q.setParameter("hoje", hoje);
        List<?> linhas = q.getResultList();
        return linhas.stream().map(l -> {
            Object[] c = (Object[]) l;
            return new LoteDisponivel((UUID) c[0], ((Number) c[1]).intValue());
        }).toList();
    }

    @Override
    public void atualizarQuantidadeLote(UUID id, int quantidade) {
        Query q = em.createNativeQuery("update lote_estoque set quantidade_unidades = :quantidade where id = :id");
        q.setParameter("quantidade", quantidade);
        q.setParameter("id", id);
        q.executeUpdate();
    }

    @Override
    public void removerLote(UUID id) {
        Query q = em.createNativeQuery("delete from lote_estoque where id = :id");
        q.setParameter("id", id);
        q.executeUpdate();
    }

    @Override
    public boolean atualizarParametros(TipoSanguineo tipo, int limiarCritico, int metaUnidades) {
        Query q = em.createNativeQuery("""
                update estoque set limiar_critico = :limiar, meta_unidades = :meta
                 where tipo_sanguineo = :tipo
                """);
        q.setParameter("limiar", limiarCritico);
        q.setParameter("meta", metaUnidades);
        q.setParameter("tipo", tipo.name());
        return q.executeUpdate() > 0;
    }

    @Override
    public int limiarCritico(TipoSanguineo tipo) {
        Query q = em.createNativeQuery("select limiar_critico from estoque where tipo_sanguineo = :tipo");
        q.setParameter("tipo", tipo.name());
        List<?> linhas = q.getResultList();
        if (linhas.isEmpty()) {
            throw new RecursoNaoEncontradoException("Tipo sanguíneo não encontrado no estoque");
        }
        return ((Number) linhas.get(0)).intValue();
    }

    @Override
    public long saldoDisponivel(TipoSanguineo tipo, LocalDate hoje) {
        Query q = em.createNativeQuery("""
                select coalesce(sum(quantidade_unidades), 0)
                  from lote_estoque
                 where tipo_sanguineo = :tipo and data_validade >= :hoje
                """);
        q.setParameter("tipo", tipo.name());
        q.setParameter("hoje", hoje);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public boolean registrarMovimentacao(Movimentacao m) {
        Query q = em.createNativeQuery("""
                insert into movimentacao_estoque (id, tipo_sanguineo, lote_id, tipo_movimentacao, quantidade,
                                                  motivo, registrado_por, chave_idempotencia, criado_em)
                values (:id, :tipo, :lote, :movimentacao, :quantidade,
                        nullif(:motivo, ''), :por, nullif(:chave, ''), now())
                on conflict (chave_idempotencia) do nothing
                """);
        q.setParameter("id", m.id());
        q.setParameter("tipo", m.tipoSanguineo().name());
        q.setParameter("lote", m.loteId());
        q.setParameter("movimentacao", m.tipo().name());
        q.setParameter("quantidade", m.quantidade());
        q.setParameter("motivo", m.motivo() == null ? "" : m.motivo().name());
        q.setParameter("por", m.registradoPor());
        q.setParameter("chave", m.chaveIdempotencia() == null ? "" : m.chaveIdempotencia());
        return q.executeUpdate() > 0;
    }

    @Override
    public Optional<Movimentacao> buscarEntradaPorChave(String chaveIdempotencia) {
        Query q = em.createNativeQuery("select " + COLUNAS_MOVIMENTACAO
                + " from movimentacao_estoque where chave_idempotencia = :chave");
        q.setParameter("chave", chaveIdempotencia);
        List<?> linhas = q.getResultList();
        return linhas.stream().map(l -> movimentacao((Object[]) l)).findFirst();
    }

    @Override
    public List<Movimentacao> listarMovimentacoes(TipoSanguineo tipo, Instant desde, Instant ate, int limite) {
        StringBuilder sql = new StringBuilder("select " + COLUNAS_MOVIMENTACAO + """
                  from movimentacao_estoque
                 where criado_em >= :desde and criado_em < :ate
                """);
        if (tipo != null) {
            sql.append(" and tipo_sanguineo = :tipo");
        }
        sql.append(" order by criado_em desc, id");
        Query q = em.createNativeQuery(sql.toString());
        q.setParameter("desde", desde.atOffset(ZoneOffset.UTC));
        q.setParameter("ate", ate.atOffset(ZoneOffset.UTC));
        if (tipo != null) {
            q.setParameter("tipo", tipo.name());
        }
        q.setMaxResults(limite);
        List<?> linhas = q.getResultList();
        return linhas.stream().map(l -> movimentacao((Object[]) l)).toList();
    }

    private static Lote lote(Object[] c) {
        return new Lote((UUID) c[0], TipoSanguineo.valueOf((String) c[1]), ((Number) c[2]).intValue(),
                data(c[3]), data(c[4]), (UUID) c[5]);
    }

    private static Movimentacao movimentacao(Object[] c) {
        return new Movimentacao((UUID) c[0], TipoSanguineo.valueOf((String) c[1]), (UUID) c[2],
                TipoMovimentacao.valueOf((String) c[3]), ((Number) c[4]).intValue(),
                c[5] == null ? null : MotivoBaixa.valueOf((String) c[5]),
                (UUID) c[6], (String) c[7], instante(c[8]));
    }

    private static LocalDate data(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return ((java.util.Date) o).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Instant instante(Object o) {
        if (o instanceof Instant i) return i;
        if (o instanceof OffsetDateTime d) return d.toInstant();
        if (o instanceof java.sql.Timestamp t) return t.toInstant();
        return ((java.util.Date) o).toInstant();
    }
}