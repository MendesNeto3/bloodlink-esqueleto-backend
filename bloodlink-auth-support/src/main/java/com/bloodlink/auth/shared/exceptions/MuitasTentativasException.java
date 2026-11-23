package com.bloodlink.auth.shared.exceptions;

/** Login bloqueado temporariamente por excesso de tentativas erradas. Vira HTTP 429 com Retry-After. */
public class MuitasTentativasException extends RuntimeException {

    private final long segundosParaLiberar;

    public MuitasTentativasException(long segundosParaLiberar) {
        super("Muitas tentativas de login. Tente novamente em " + minutos(segundosParaLiberar) + " minuto(s)");
        this.segundosParaLiberar = segundosParaLiberar;
    }

    public long segundosParaLiberar() {
        return segundosParaLiberar;
    }

    private static long minutos(long segundos) {
        return Math.max(1, (segundos + 59) / 60);
    }
}
