package com.bloodlink.api.agendamento.adapter.out.persistence;

import com.bloodlink.api.agendamento.domain.Agendamento;
import com.bloodlink.api.agendamento.domain.OrigemAgendamento;
import com.bloodlink.api.agendamento.domain.StatusAgendamento;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "agendamento")
public class AgendamentoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "doador_id", nullable = false, updatable = false)
    private UUID doadorId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime hora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusAgendamento status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private OrigemAgendamento origem;

    @Column(length = 500)
    private String observacao;

    @Column(name = "motivo_cancelamento", length = 300)
    private String motivoCancelamento;

    @Column(name = "solicitado_por", nullable = false, updatable = false)
    private UUID solicitadoPor;

    @Column(name = "doacao_id")
    private UUID doacaoId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected AgendamentoJpaEntity() {
    }

    public static AgendamentoJpaEntity de(Agendamento a) {
        AgendamentoJpaEntity e = new AgendamentoJpaEntity();
        e.id = a.id();
        e.doadorId = a.doadorId();
        e.data = a.date();
        e.hora = a.hora();
        e.status = a.status();
        e.origem = a.origem();
        e.observacao = a.observacao();
        e.motivoCancelamento = a.motivoCancelamento();
        e.solicitadoPor = a.solicitadoPor();
        e.doacaoId = a.doacaoId();
        e.criadoEm = a.criadoEm();
        e.atualizadoEm = a.atualizadoEm();
        return e;
    }

    public Agendamento paraDominio() {
        return new Agendamento(id, doadorId, data, hora, status, origem, observacao, motivoCancelamento,
                solicitadoPor, doacaoId, criadoEm, atualizadoEm);
    }

    @Override
    public String toString() {
        return "AgendamentoJpaEntity[id=" + id + "]";
    }
}

