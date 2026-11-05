package com.bloodlink.api.agendamento.adapter.out.persistence;

import com.bloodlink.api.agendamento.domain.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;


public interface AgendamentoJpaRepository
        extends JpaRepository<AgendamentoJpaEntity, UUID>, JpaSpecificationExecutor<AgendamentoJpaEntity> {

    Optional<AgendamentoJpaEntity> findFirstByDoadorIdAndStatusIn(UUID doadorId, Collection<StatusAgendamento> status);

    List<AgendamentoJpaEntity> findByDoadorIdOrderByDataDescHoraDesc(UUID doadorId);

    long countByDataAndHoraAndStatusIn(LocalDate data, LocalTime hora, Collection<StatusAgendamento> status);

    @Query("""
            select a.hora as hora, count(a) as total
            from AgendamentoJpaEntity a
            where a.data = :data and a.status in :status
            group by a.hora
            """)
    List<ContagemHorario> contarPorHorario(@Param("data") LocalDate data,
                                           @Param("status") Collection<StatusAgendamento> status);

    interface ContagemHorario {
        LocalTime getHora();

        long getTotal();
    }
}