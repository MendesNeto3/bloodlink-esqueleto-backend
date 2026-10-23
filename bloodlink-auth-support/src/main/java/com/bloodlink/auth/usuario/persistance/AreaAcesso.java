package com.bloodlink.auth.usuario.persistance;

import com.bloodlink.auth.usuario.Papel;

public enum AreaAcesso {
    DOADOR, HEMOCENTRO;

    public boolean permite(Papel papel) {
        return this == DOADOR ? papel == Papel.DOADOR : papel != Papel.DOADOR;
    }
}