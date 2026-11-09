package com.bloodlink.api.painel;

import com.bloodlink.api.agendamento.domain.StatusAgendamento;
import com.bloodlink.api.painel.domain.PainelResumo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class PainelRepositoryAdapter implements PainelRepositoryPort {

    @PersistenceContext
    private EntityManager em;

    @Override
    public long contarDoacoes(LocalDate inicio, LocalDate fim) {
        Query q = em.createNativeQuery("select count(*) from doacao where data between :ini and :fim");
        q.setParameter("ini", inicio);
        q.setParameter("fim", fim);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public long contarDoadoresComDoacaoDesde(LocalDate desde) {
        Query q = em.createNativeQuery("""
                select count(distinct o.doador_id)
                  from doacao o join doador d on d.id = o.doador_id
                 where d.ativo = true and o.data >= :desde
                """);
        q.setParameter("desde", desde);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public long contarDoadoresRecorrentesDesde(LocalDate desde) {
        Query q = em.createNativeQuery("""
                select count(*) from (
                    select o.doador_id
                      from doacao o join doador d on d.id = o.doador_id
                     where d.ativo = true and o.data >= :desde
                     group by o.doador_id
                    having count(*) >= 2
                ) t
                """);
        q.setParameter("desde", desde);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public long contarAgendamentos(LocalDate inicio, LocalDate fim, Collection<StatusAgendamento> status) {
        Query q = em.createNativeQuery("""
                select count(*) from agendamento
                 where data between :ini and :fim and status in (:status)
                """);
        q.setParameter("ini", inicio);
        q.setParameter("fim", fim);
        q.setParameter("status", status.stream().map(Enum::name).toList());
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public List<Integer> diasComColeta(LocalDate inicio, LocalDate fim) {
        Query q = em.createNativeQuery("""
                select distinct cast(extract(day from data) as integer) as dia
                  from agendamento
                 where data between :ini and :fim and status in ('CONFIRMADO', 'CONCLUIDO')
                 order by dia
                """);
        q.setParameter("ini", inicio);
        q.setParameter("fim", fim);
        List<?> linhas = q.getResultList();
        return linhas.stream().map(o -> ((Number) o).intValue()).toList();
    }

    @Override
    public List<PainelResumo.ProximaColeta> proximasColetas(LocalDate aPartirDe, int limite) {
        Query q = em.createNativeQuery("""
                select a.id, a.doador_id, d.nome, a.data, a.hora
                  from agendamento a join doador d on d.id = a.doador_id
                 where a.status = 'CONFIRMADO' and a.data >= :de
                 order by a.data, a.hora
                """);
        q.setParameter("de", aPartirDe);
        q.setMaxResults(limite);
        List<?> linhas = q.getResultList();
        return linhas.stream().map(l -> {
            Object[] c = (Object[]) l;
            return new PainelResumo.ProximaColeta((UUID) c[0], (UUID) c[1], (String) c[2],
                    data(c[3]), hora(c[4]));
        }).toList();
    }

    @Override
    public List<UUID> idsDoadoresRecentes(int limite) {
        Query q = em.createNativeQuery("""
                select d.id
                  from doador d join doacao o on o.doador_id = d.id
                 where d.ativo = true
                 group by d.id, d.nome
                 order by max(o.data) desc, d.nome
                """);
        q.setMaxResults(limite);
        List<?> linhas = q.getResultList();
        return linhas.stream().map(o -> (UUID) o).toList();
    }

    private static LocalDate data(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return ((java.util.Date) o).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static LocalTime hora(Object o) {
        if (o instanceof LocalTime t) return t;
        return ((java.sql.Time) o).toLocalTime();
    }
}