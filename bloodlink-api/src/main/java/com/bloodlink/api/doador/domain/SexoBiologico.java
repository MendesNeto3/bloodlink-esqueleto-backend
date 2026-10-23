package com.bloodlink.api.doador.domain;

public enum SexoBiologico {
    MASCULINO(60, 4),
    FEMININO(90, 3);

    private final int intervaloMinimoDias;
    private final int maxDoacoesPorAno;

    SexoBiologico(int intervaloMinimoDias, int maxDoacoesPorAno) {
        this.intervaloMinimoDias = intervaloMinimoDias;
        this.maxDoacoesPorAno = maxDoacoesPorAno;
    }

    public int intervaloMinimoDias() {
        return intervaloMinimoDias;
    }

    public int maxDoacoesPorAno() {
        return maxDoacoesPorAno;
    }
}