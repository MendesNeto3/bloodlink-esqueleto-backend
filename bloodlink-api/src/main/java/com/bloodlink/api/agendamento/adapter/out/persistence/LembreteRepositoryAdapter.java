package com.bloodlink.api.agendamento.adapter.out.persistence;

import com.bloodlink.api.agendamento.application.port.out.LembreteRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** SQL nativo pelo EntityManager: respeita o schema do tenant da requisição (ou do job). */
@Component
public class LembreteRepositoryAdapter implements LembreteRepositoryPort {

    /** Teto por execução e por tenant; o limite vai no SQL porque tem que vir antes do "for update". */
    private static final int LIMITE_POR_EXECUCAO = 500;

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<AgendamentoParaLembrete> reservarPendentes(LocalDate data) {
        List<?> linhas = em.createNativeQuery("""
                        select a.id, a.doador_id, d.nome, d.email, a.data, a.hora
                          from agendamento a
                          join doador d on d.id = a.doador_id
                         where a.data = :data
                           and a.status = 'CONFIRMADO'
                           and a.lembrete_enviado_em is null
                         order by a.hora, a.id
                         limit %d
                           for update of a skip locked
                        """.formatted(LIMITE_POR_EXECUCAO))
                .setParameter("data", data)
                .getResultList();
        return linhas.stream().map(l -> {
            Object[] c = (Object[]) l;
            return new AgendamentoParaLembrete(uuid(c[0]), uuid(c[1]), (String) c[2], (String) c[3],
                    data(c[4]), hora(c[5]));
        }).toList();
    }

    @Override
    public void marcarEnviado(UUID agendamentoId, Instant quando) {
        em.createNativeQuery("update agendamento set lembrete_enviado_em = :quando where id = :id")
                .setParameter("quando", OffsetDateTime.ofInstant(quando, ZoneOffset.UTC))
                .setParameter("id", agendamentoId)
                .executeUpdate();
    }

    private static UUID uuid(Object o) {
        return o instanceof UUID u ? u : UUID.fromString(o.toString());
    }

    private static LocalDate data(Object o) {
        if (o instanceof LocalDate d) {
            return d;
        }
        if (o instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (o instanceof java.util.Date d) {
            return new java.sql.Date(d.getTime()).toLocalDate();
        }
        return LocalDate.parse(o.toString());
    }

    private static LocalTime hora(Object o) {
        if (o instanceof LocalTime t) {
            return t;
        }
        if (o instanceof java.sql.Time t) {
            return t.toLocalTime();
        }
        return LocalTime.parse(o.toString());
    }
}
