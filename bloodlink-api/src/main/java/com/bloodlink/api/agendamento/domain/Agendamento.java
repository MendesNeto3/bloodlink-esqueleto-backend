package com.bloodlink.api.agendamento.domain;

import com.bloodlink.auth.shared.exceptions.ConflitoException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record Agendamento(UUID id,
                          UUID doadorId,
                          LocalDate date,
                          LocalTime hora,
                          StatusAgendamento status,
                          OrigemAgendamento origem,
                          String observacao,
                          String motivoCancelamento,
                          UUID solicitadoPor,
                          UUID doacaoId,
                          Instant criadoEm,
                          Instant atualizadoEm) {

    public static Agendamento novo(UUID doadorId, LocalDate data, LocalTime hora, OrigemAgendamento origem,
                                   String observacao, UUID solicitadoPor, Instant agora) {
        return new Agendamento(UUID.randomUUID(), doadorId, data, hora, StatusAgendamento.PENDENTE, origem,
                observacao, null, solicitadoPor, null, agora, agora);
    }

    public Agendamento confirmar(Instant agora) {
        if (status != StatusAgendamento.PENDENTE) {
            throw new ConflitoException("Só é possível confirmar um agendamento pendente");
        }
        return new Agendamento(id, doadorId, date, hora, StatusAgendamento.CONFIRMADO, origem, observacao,
                motivoCancelamento, solicitadoPor, doacaoId, criadoEm, agora);
    }

    public Agendamento cancelar(String motivo, Instant agora) {
        if (!status.ativo()) {
            throw new ConflitoException("Só é possível cancelar um agendamento pendente ou confirmado");
        }
        return new Agendamento(id, doadorId, date, hora, StatusAgendamento.CANCELADO, origem, observacao,
                motivo, solicitadoPor, doacaoId, criadoEm, agora);
    }

    @Override
    public String toString() {
        return "Agendamento[id=" + id + "]";
    }
}

