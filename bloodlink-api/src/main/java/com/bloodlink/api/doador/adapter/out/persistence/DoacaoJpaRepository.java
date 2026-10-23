package com.bloodlink.api.doador.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DoacaoJpaRepository extends JpaRepository<DoacaoJpaEntity, UUID> {

    boolean existsByDoadorIdAndData(UUID doadorId, LocalDate data);

    List<DoacaoJpaEntity> findByDoadorIdOrderByDataDesc(UUID doadorId);

    interface DataDoacao {
        UUID getDoadorId();

        LocalDate getData();
    }

    @Query("select d.doadorId as doadorId, d.data as data from DoacaoJpaEntity d where d.doadorId in :ids")
    List<DataDoacao> buscarDatas(@Param("ids") Collection<UUID> ids);
}
