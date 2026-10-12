package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.out.FiltroDoador;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.shared.Exception.ConflitoException;
import com.bloodlink.api.shared.Pagina.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.*;
import jakarta.persistence.criteria.Predicate;

@Component
public class DoadorRepositoryAdapter implements DoadorRepositoryPort {

    private final DoadorJpaRepository doadorJpaRepository;

    public DoadorRepositoryAdapter(DoadorJpaRepository doadorJpaRepository) {
        this.doadorJpaRepository = doadorJpaRepository;
    }

    @Override
    public Doador salvar(Doador doador) {
        try {
            // saveAndFlush: a violação do UNIQUE estoura aqui dentro, onde dá para traduzir,
            // e não no commit da transação do service
            return doadorJpaRepository.saveAndFlush(DoadorJpaEntity.de(doador)).paraDominio();
        } catch (RuntimeException e) {
            if (violou(e, "uk_doador_cpf")) {
                throw new ConflitoException("Já existe um doador cadastrado com este CPF");
            }
            throw e;
        }
    }

    @Override
    public Optional<Doador> buscarPorId(UUID id) {
        return doadorJpaRepository.findById(id).map(DoadorJpaEntity::paraDominio);
    }

    @Override
    public Optional<Doador> buscarPorCpf(String cpf) {
        return doadorJpaRepository.findByCpf(cpf).map(DoadorJpaEntity::paraDominio);
    }

    @Override
    public boolean existePorCpf(String cpf) {
        return doadorJpaRepository.existsByCpf(cpf);
    }

    @Override
    public Pagina<Doador> listar(FiltroDoador filtro, int pagina, int tamanho) {
        Specification<DoadorJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (filtro.nome() != null) {
                String padrao = "%" + escaparLike(filtro.nome().toLowerCase(Locale.ROOT)) + "%";
                predicados.add(cb.like(cb.lower(root.<String>get("nome")), padrao, '\\'));
            }
            if (filtro.tipoSanguineo() != null) {
                predicados.add(cb.equal(root.get("tipoSanguineo"), filtro.tipoSanguineo()));
            }
            if (filtro.ativo() != null) {
                predicados.add(cb.equal(root.get("ativo"), filtro.ativo()));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };

        Pageable pageable = PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Order.asc("nome"), Sort.Order.asc("id"))); // id desempata: paginação estável
        Page<DoadorJpaEntity> resultado = doadorJpaRepository.findAll(spec, pageable);

        return new Pagina<>(
                resultado.getContent().stream().map(DoadorJpaEntity::paraDominio).toList(),
                resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static boolean violou(Throwable erro, String constraint) {
        for (Throwable t = erro; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(constraint)) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
