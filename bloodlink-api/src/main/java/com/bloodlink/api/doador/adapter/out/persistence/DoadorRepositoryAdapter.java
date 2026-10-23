package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.out.FiltroDoador;
import com.bloodlink.api.doador.domain.Doacao;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.api.shared.Pagina.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;

@Component
public class DoadorRepositoryAdapter implements DoadorRepositoryPort, DoacaoRepositoryPort {

    private final DoadorJpaRepository doadorJpaRepository;
    private final DoacaoJpaRepository doacaoJpaRepository;

    public DoadorRepositoryAdapter(DoadorJpaRepository doadorJpaRepository,
                                   DoacaoJpaRepository doacaoJpaRepository) {
        this.doadorJpaRepository = doadorJpaRepository;
        this.doacaoJpaRepository = doacaoJpaRepository;
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
    public Optional<Doador> buscarPorEmail(String email) {
        return unico(doadorJpaRepository.findTop2ByEmail(email), "e-mail");
    }

    @Override
    public Optional<Doador> buscarPorNome(String nome) {
        return unico(doadorJpaRepository.findTop2ByNomeIgnoreCase(nome), "nome");
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

    // ---------- Doação ----------

    @Override
    public Doacao salvar(Doacao doacao) {
        try {
            return doacaoJpaRepository.saveAndFlush(DoacaoJpaEntity.de(doacao)).paraDominio();
        } catch (RuntimeException e) {
            if (violou(e, "uk_doacao_doador_data")) {
                throw new ConflitoException("Já existe uma doação registrada para este doador nesta data");
            }
            throw e;
        }
    }

    @Override
    public boolean existePorDoadorEData(UUID doadorId, LocalDate data) {
        return doacaoJpaRepository.existsByDoadorIdAndData(doadorId, data);
    }

    @Override
    public List<Doacao> listarPorDoador(UUID doadorId) {
        return doacaoJpaRepository.findByDoadorIdOrderByDataDesc(doadorId).stream()
                .map(DoacaoJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public Map<UUID, List<LocalDate>> datasPorDoadores(Collection<UUID> doadorIds) {
        if (doadorIds.isEmpty()) {
            return Map.of();
        }
        return doacaoJpaRepository.buscarDatas(doadorIds).stream()
                .collect(Collectors.groupingBy(
                        DoacaoJpaRepository.DataDoacao::getDoadorId,
                        Collectors.mapping(DoacaoJpaRepository.DataDoacao::getData, Collectors.toList())));
    }

    // ---------- auxiliares ----------

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

    private Optional<Doador> unico(List<DoadorJpaEntity> encontrados, String criterio) {
        if (encontrados.size() > 1) {
            throw new ConflitoException(
                    "Há mais de um doador com este " + criterio + "; consulte pelo CPF ou use a listagem com filtros");
        }
        return encontrados.stream().findFirst().map(DoadorJpaEntity::paraDominio);
    }
}