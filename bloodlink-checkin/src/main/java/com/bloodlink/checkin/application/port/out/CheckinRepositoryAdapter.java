package com.bloodlink.checkin.adapter.out.persistence;

import com.bloodlink.checkin.application.port.out.CheckinRepositoryPort;
import com.bloodlink.checkin.domain.AgendamentoParaCheckin;
import com.bloodlink.checkin.domain.Checkin;
import com.bloodlink.checkin.domain.ItemFila;
import com.bloodlink.checkin.domain.ResultadoTriagem;
import com.bloodlink.checkin.domain.SituacaoCheckin;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** SQL nativo pelo EntityManager: respeita o schema do tenant da requisição. */
@Component
public class CheckinRepositoryAdapter implements CheckinRepositoryPort {

    private static final String COLUNAS_CHECKIN = """
            select id, agendamento_id, doador_id, chegada_em, registrado_por,
                   triagem_resultado, triagem_observacao, triagem_em, triagem_por
              from checkin
             where id = :id
            """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<AgendamentoParaCheckin> buscarAgendamento(UUID id) {
        List<?> linhas = em.createNativeQuery(
                        "select id, doador_id, data, status from agendamento where id = :id for share")
                .setParameter("id", id)
                .getResultList();
        if (linhas.isEmpty()) {
            return Optional.empty();
        }
        Object[] c = (Object[]) linhas.get(0);
        return Optional.of(new AgendamentoParaCheckin(uuid(c[0]), uuid(c[1]), data(c[2]), (String) c[3]));
    }

    @Override
    public boolean inserir(Checkin checkin) {
        int linhas = em.createNativeQuery("""
                        insert into checkin (id, agendamento_id, doador_id, chegada_em, registrado_por)
                        values (:id, :agendamento, :doador, :chegada, :por)
                        on conflict (agendamento_id) do nothing
                        """)
                .setParameter("id", checkin.id())
                .setParameter("agendamento", checkin.agendamentoId())
                .setParameter("doador", checkin.doadorId())
                .setParameter("chegada", utc(checkin.chegadaEm()))
                .setParameter("por", checkin.registradoPor())
                .executeUpdate();
        return linhas == 1;
    }

    @Override
    public Optional<Checkin> buscarPorId(UUID id) {
        return buscarCheckin(COLUNAS_CHECKIN, id);
    }

    @Override
    public Optional<Checkin> buscarPorIdParaAtualizar(UUID id) {
        return buscarCheckin(COLUNAS_CHECKIN + " for update", id);
    }

    @Override
    public boolean atualizarTriagem(Checkin checkin) {
        // observação vazia vira NULL no banco (nullif); evita enviar parâmetro nulo sem tipo
        int linhas = em.createNativeQuery("""
                        update checkin
                           set triagem_resultado = :resultado,
                               triagem_observacao = nullif(:observacao, ''),
                               triagem_em = :em,
                               triagem_por = :por
                         where id = :id and triagem_resultado is null
                        """)
                .setParameter("resultado", checkin.triagem().name())
                .setParameter("observacao", checkin.triagemObservacao() == null ? "" : checkin.triagemObservacao())
                .setParameter("em", utc(checkin.triagemEm()))
                .setParameter("por", checkin.triagemPor())
                .setParameter("id", checkin.id())
                .executeUpdate();
        return linhas == 1;
    }

    @Override
    public List<ItemFila> fila(LocalDate data) {
        List<?> linhas = em.createNativeQuery("""
                        select a.id, c.id, a.doador_id, d.nome, d.tipo_sanguineo, a.hora, a.status,
                               c.chegada_em, c.triagem_resultado, c.triagem_observacao, c.triagem_em
                          from agendamento a
                          join doador d on d.id = a.doador_id
                          left join checkin c on c.agendamento_id = a.id
                         where a.data = :data
                           and (a.status in ('CONFIRMADO', 'CONCLUIDO', 'FALTOU') or c.id is not null)
                         order by a.hora, lower(d.nome), a.id
                        """)
                .setParameter("data", data)
                .getResultList();
        return linhas.stream().map(l -> {
            Object[] c = (Object[]) l;
            String status = (String) c[6];
            ResultadoTriagem triagem = c[8] == null ? null : ResultadoTriagem.valueOf((String) c[8]);
            return new ItemFila(uuid(c[0]), uuid(c[1]), uuid(c[2]), (String) c[3], (String) c[4],
                    hora(c[5]), status, SituacaoCheckin.de(status, c[1] != null, triagem),
                    instante(c[7]), triagem, (String) c[9], instante(c[10]));
        }).toList();
    }

    private Optional<Checkin> buscarCheckin(String sql, UUID id) {
        List<?> linhas = em.createNativeQuery(sql).setParameter("id", id).getResultList();
        if (linhas.isEmpty()) {
            return Optional.empty();
        }
        Object[] c = (Object[]) linhas.get(0);
        return Optional.of(new Checkin(uuid(c[0]), uuid(c[1]), uuid(c[2]), instante(c[3]), uuid(c[4]),
                c[5] == null ? null : ResultadoTriagem.valueOf((String) c[5]),
                (String) c[6], instante(c[7]), uuid(c[8])));
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static UUID uuid(Object o) {
        if (o == null) {
            return null;
        }
        return o instanceof UUID u ? u : UUID.fromString(o.toString());
    }

    private static LocalDate data(Object o) {
        if (o == null) {
            return null;
        }
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
        if (o == null) {
            return null;
        }
        if (o instanceof LocalTime t) {
            return t;
        }
        if (o instanceof java.sql.Time t) {
            return t.toLocalTime();
        }
        return LocalTime.parse(o.toString());
    }

    private static Instant instante(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Instant i) {
            return i;
        }
        if (o instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (o instanceof ZonedDateTime z) {
            return z.toInstant();
        }
        if (o instanceof java.util.Date d) {
            return Instant.ofEpochMilli(d.getTime());
        }
        throw new IllegalStateException("Tipo de data/hora inesperado: " + o.getClass().getSimpleName());
    }
}