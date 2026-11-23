package com.bloodlink.auth.usuario.persistance;

import com.bloodlink.auth.usuario.Papel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AreaAcessoTest {

    @Test
    @DisplayName("área do doador aceita só o papel DOADOR")
    void areaDoador() {
        assertThat(AreaAcesso.DOADOR.permite(Papel.DOADOR)).isTrue();
        for (Papel p : new Papel[]{Papel.ADMIN_PLATAFORMA, Papel.ADMIN_CENTRO, Papel.RECEPCAO, Papel.ENFERMEIRO}) {
            assertThat(AreaAcesso.DOADOR.permite(p)).as(p.name()).isFalse();
        }
    }

    @Test
    @DisplayName("área do hemocentro aceita toda a equipe e recusa o doador")
    void areaHemocentro() {
        assertThat(AreaAcesso.HEMOCENTRO.permite(Papel.DOADOR)).isFalse();
        for (Papel p : new Papel[]{Papel.ADMIN_PLATAFORMA, Papel.ADMIN_CENTRO, Papel.RECEPCAO, Papel.ENFERMEIRO}) {
            assertThat(AreaAcesso.HEMOCENTRO.permite(p)).as(p.name()).isTrue();
        }
    }
}
