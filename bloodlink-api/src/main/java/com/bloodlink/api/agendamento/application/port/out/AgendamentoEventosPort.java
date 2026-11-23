package com.bloodlink.api.agendamento.application.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

/** Avisa o resto do sistema (Notificação) sobre o que aconteceu com um agendamento. */
public interface AgendamentoEventosPort {

    /** Avisa a confirmação. Não pode falhar a operação que o chamou: erro de publicação só vai para o log. */
    void confirmado(DadosAgendamentoEvento dados);

    /** Publica o lembrete do dia. Devolve false se não conseguiu publicar (o lembrete fica pendente). */
    boolean lembrete(DadosAgendamentoEvento dados);

    record DadosAgendamentoEvento(UUID agendamentoId, UUID doadorId, String doadorNome, String doadorEmail,
                                  String centroNome, LocalDateTime dataHora) {

        /** Nome e e-mail do doador são dados pessoais (LGPD): nunca vão para log. */
        @Override
        public String toString() {
            return "DadosAgendamentoEvento[agendamentoId=" + agendamentoId + "]";
        }
    }
}
