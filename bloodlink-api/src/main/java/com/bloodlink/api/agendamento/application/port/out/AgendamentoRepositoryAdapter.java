package com.bloodlink.api.agendamento.application.port.out;

import com.bloodlink.api.agendamento.adapter.out.persistence.AgendamentoJpaEntity;
import com.bloodlink.api.agendamento.adapter.out.persistence.AgendamentoJpaRepository;
import com.bloodlink.api.agendamento.application.port.in.AgendamentoRepositoryPort;
import com.bloodlink.api.agendamento.domain.Agendamento;
import com.bloodlink.api.agendamento.domain.FiltroAgendamento;
import com.bloodlink.api.agendamento.domain.StatusAgendamento;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AgendamentoRepositoryAdapter implements AgendamentoRepositoryPort {

    private static final List<StatusAgendamento> ATIVOS = List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO);
    private final AgendamentoJpaRepository jpa;

    public AgendamentoRepositoryAdapter(AgendamentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Agendamento salvar(Agendamento agendamento) {
        try {
            // saveAndFlush: a violação do índice único estoura aqui dentro, onde dá para traduzir
            return jpa.saveAndFlush(AgendamentoJpaEntity.de(agendamento)).paraDominio();
        } catch (RuntimeException e) {
            if (violou(e, "uk_agendamento_ativo_por_doador")) {
                throw new ConflitoException("Este doador já tem um agendamento ativo");
            }
            throw e;
        }
    }

    @Override
    public Optional<Agendamento> buscarPorId(UUID id) {
        return jpa.findById(id).map(AgendamentoJpaEntity::paraDominio);
    }

    @Override
    public Optional<Agendamento> buscarAtivoDoDoador(UUID doadorId) {
        return jpa.findFirstByDoadorIdAndStatusIn(doadorId, ATIVOS).map(AgendamentoJpaEntity::paraDominio);
    }


    @Override
    public List<Agendamento> listarPorDoador(UUID doadorId) {
        return jpa.findByDoadorIdOrderByDataDescHoraDesc(doadorId).stream()
                .map(AgendamentoJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public long contarAtivosNoHorario(LocalDate data, LocalTime hora) {
        return jpa.countByDataAndHoraAndStatusIn(data, hora, ATIVOS);
    }

    @Override
    public Map<LocalTime, Long> contarAtivosPorHorario(LocalDate data) {
        return jpa.contarPorHorario(data, ATIVOS).stream()
                .collect(Collectors.toMap(
                        AgendamentoJpaRepository.ContagemHorario::getHora,
                        AgendamentoJpaRepository.ContagemHorario::getTotal));
    }

    @Override
    public List<Agendamento> listar(FiltroAgendamento filtroAgendamento, int limite) {
        Specification<AgendamentoJpaEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicados = new ArrayList<>();
            if (filtroAgendamento.de() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("data"), filtroAgendamento.de()));
            }
            if (filtroAgendamento.ate() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.<LocalDate>get("data"), filtroAgendamento.ate()));
            }
            if (filtroAgendamento.status() != null) {
                predicados.add(cb.equal(root.get("status"), filtroAgendamento.status()));
            }
            if (filtroAgendamento.doadorId() != null) {
                predicados.add(cb.equal(root.get("doadorId"), filtroAgendamento.doadorId()));
            }
            return cb.and();
        };
        Pageable pageable = PageRequest.of(0, limite,
                Sort.by(Sort.Order.asc("data"), Sort.Order.asc("hora"), Sort.Order.asc("id")));
        return jpa.findAll(spec, pageable).getContent().stream()
                .map(AgendamentoJpaEntity::paraDominio)
                .toList();
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

