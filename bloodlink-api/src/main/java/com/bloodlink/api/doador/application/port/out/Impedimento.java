package com.bloodlink.api.doador.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Impedimento clínico registrado pela equipe. Definitivo não tem data final; temporário vale até "fim" (inclusive).
 * "encerradoEm" preenchido = impedimento revogado pela equipe (deixa de valer).
 */
public record Impedimento(
        UUID id,
        UUID doadorId,
        boolean definitivo,
        LocalDate inicio,
        LocalDate fim,
        String motivo,
        UUID registradoPor,
        Instant criadoEm,
        Instant encerradoEm
) {

    public Impedimento {
        if (inicio == null) {
            throw new IllegalArgumentException("O início do impedimento é obrigatório");
        }
        if (definitivo && fim != null) {
            throw new IllegalArgumentException("Impedimento definitivo não tem data final");
        }
        if (!definitivo && fim == null) {
            throw new IllegalArgumentException("Impedimento temporário exige a data final");
        }
        if (!definitivo && fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final não pode ser anterior ao início");
        }
    }

    public Impedimento encerrado(Instant agora) {
        return new Impedimento(id, doadorId, definitivo, inicio, fim, motivo, registradoPor, criadoEm, agora);
    }

    public boolean vigenteEm(LocalDate dia) {
        return encerradoEm == null
                && !inicio.isAfter(dia)
                && (definitivo || !fim.isBefore(dia));
    }

    /** O motivo é dado de saúde: nunca sai em log. */
    @Override
    public String toString() {
        return "Impedimento[id=" + id + ", doadorId=" + doadorId + ", definitivo=" + definitivo + "]";
    }
}
