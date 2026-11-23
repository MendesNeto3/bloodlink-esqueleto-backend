package com.bloodlink.api.agendamento.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface LembreteRepositoryPort {

    /**
     * Agendamentos confirmados da data que ainda não receberam o lembrete, já travados (for update skip locked)
     * até o fim da transação: duas execuções ao mesmo tempo nunca pegam o mesmo agendamento.
     */
    List<AgendamentoParaLembrete> reservarPendentes(LocalDate data);

    void marcarEnviado(UUID agendamentoId, Instant quando);

    record AgendamentoParaLembrete(UUID agendamentoId, UUID doadorId, String doadorNome, String doadorEmail,
                                   LocalDate data, LocalTime hora) {

        /** Nome e e-mail do doador são dados pessoais (LGPD): nunca vão para log. */
        @Override
        public String toString() {
            return "AgendamentoParaLembrete[agendamentoId=" + agendamentoId + "]";
        }
    }
}
