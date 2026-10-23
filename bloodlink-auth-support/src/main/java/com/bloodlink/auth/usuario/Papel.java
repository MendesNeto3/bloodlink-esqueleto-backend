package com.bloodlink.auth.usuario;

public enum Papel {
    ADMIN_PLATAFORMA(false),
    ADMIN_CENTRO(true),
    RECEPCAO(true),
    ENFERMEIRO(true),
    DOADOR(true);

    private final boolean exigeCentro;

    Papel(boolean exigeCentro) {
        this.exigeCentro = exigeCentro;
    }

    public boolean exigeCentro() {
        return exigeCentro;
    }

    /** Só a plataforma e o administrador do centro gerenciam (listam, cadastram, inativam) usuários. */
    public boolean gerenciaUsuarios() {
        return this == ADMIN_PLATAFORMA || this == ADMIN_CENTRO;
    }

    /** Papéis da equipe do centro, cadastrados com senha por esta rota; o DOADOR nasce pelo fluxo de ativação. */
    public boolean ehEquipeDoCentro() {
        return this == ADMIN_CENTRO || this == RECEPCAO || this == ENFERMEIRO;
    }
}
