package com.bloodlink.api.doador.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TipoSanguineoESexoTest {

    @Test
    @DisplayName("TipoSanguineo.de aceita rótulo, nome do enum, minúsculas e espaços")
    void tipoAceitaFormas() {
        assertThat(TipoSanguineo.de("O-")).contains(TipoSanguineo.O_NEGATIVO);
        assertThat(TipoSanguineo.de("o-")).contains(TipoSanguineo.O_NEGATIVO);
        assertThat(TipoSanguineo.de(" ab+ ")).contains(TipoSanguineo.AB_POSITIVO);
        assertThat(TipoSanguineo.de("A_POSITIVO")).contains(TipoSanguineo.A_POSITIVO);
    }

    @Test
    @DisplayName("TipoSanguineo.de devolve vazio para nulo, branco e valores desconhecidos (inclusive 'Não sei')")
    void tipoRejeita() {
        assertThat(TipoSanguineo.de(null)).isEmpty();
        assertThat(TipoSanguineo.de("  ")).isEmpty();
        assertThat(TipoSanguineo.de("C+")).isEmpty();
        assertThat(TipoSanguineo.de("Não sei")).isEmpty();
    }

    @Test
    @DisplayName("todos os oito tipos têm rótulo único e reconhecível")
    void rotulosUnicos() {
        assertThat(TipoSanguineo.values()).hasSize(8);
        for (TipoSanguineo t : TipoSanguineo.values()) {
            assertThat(TipoSanguineo.de(t.rotulo())).contains(t);
        }
    }

    @Test
    @DisplayName("regras por sexo biológico: intervalo e limite anual")
    void regrasPorSexo() {
        assertThat(SexoBiologico.MASCULINO.intervaloMinimoDias()).isEqualTo(60);
        assertThat(SexoBiologico.MASCULINO.maxDoacoesPorAno()).isEqualTo(4);
        assertThat(SexoBiologico.FEMININO.intervaloMinimoDias()).isEqualTo(90);
        assertThat(SexoBiologico.FEMININO.maxDoacoesPorAno()).isEqualTo(3);
    }
}
