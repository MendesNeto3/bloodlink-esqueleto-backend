package com.bloodlink.api.doador.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoadorJpaRepository extends
    JpaRepository<DoadorJpaEntity, UUID>, JpaSpecificationExecutor<DoadorJpaEntity> {

    Optional<DoadorJpaEntity> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    List<DoadorJpaEntity> findTop2ByEmail(String email);

    List<DoadorJpaEntity> findTop2ByNomeIgnoreCase(String nome);

    @Query("""
            select d from DoadorJpaEntity d
            where (:ativo is null or d.ativo = :ativo)
              and (lower(d.nome) like :busca or d.cpf like :cpfParcial)
            order by lower(d.nome)
            """)
    List<DoadorJpaEntity> listar(@Param("busca") String busca,
                                 @Param("cpfParcial") String cpfParcial,
                                 @Param("ativo") Boolean ativo);
}
