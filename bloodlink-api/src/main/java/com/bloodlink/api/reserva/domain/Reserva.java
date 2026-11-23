package com.bloodlink.api.reserva.domain;

import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.auth.shared.exceptions.ConflitoException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public record Reserva(UUID id,
                      String hospital,
                      TipoSanguineo tipoSanguineo,
                      int unidades,
                      LocalDate validaAte,
                      StatusReserva status,
                      UUID criadoPor,
                      Instant criadoEm,
                      Instant liberadaEm) {

    public static Reserva nova(String hospital, TipoSanguineo tipo, int unidades, LocalDate validaAte,
                               UUID criadoPor, Instant agora) {
        return new Reserva(UUID.randomUUID(), hospital, tipo, unidades, validaAte, StatusReserva.ATIVA,
                criadoPor, agora, null);
    }

    /** Vale até o fim do dia de validade: só vence quando a data já passou. */
    public SituacaoReserva situacao(LocalDate hoje) {
        if (status == StatusReserva.LIBERADA) {
            return SituacaoReserva.LIBERADA;
        }
        return validaAte.isBefore(hoje) ? SituacaoReserva.VENCIDA : SituacaoReserva.ATIVA;
    }

    public Reserva liberar(LocalDate hoje, Instant agora) {
        if (situacao(hoje) != SituacaoReserva.ATIVA) {
            throw new ConflitoException("Só é possível liberar uma reserva ativa");
        }
        return new Reserva(id, hospital, tipoSanguineo, unidades, validaAte, StatusReserva.LIBERADA,
                criadoPor, criadoEm, agora);
    }

    /** Código curto para leitura (ex.: RS-7F3A2C91). Deriva do id. */
    public String codigo() {
        return "RS-" + id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "Reserva[id=" + id + "]";
    }
}
