package com.bloodlink.estoque.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResumoEstoqueTest {

    private static SituacaoTipo tipo(TipoSanguineo t, long unidades, int meta, int limiar, long vencendo) {
        return new SituacaoTipo(t, unidades, meta, limiar, vencendo);
    }

    private static ResumoEstoque.PorTipo unico(SituacaoTipo s) {
        return ResumoEstoque.de(List.of(s)).porTipo().get(0);
    }

    @Test
    @DisplayName("situação: ESTAVEL a partir de 70% da meta")
    void estavel() {
        assertThat(unico(tipo(TipoSanguineo.A_POSITIVO, 70, 100, 10, 0)).situacao()).isEqualTo("ESTAVEL");
        assertThat(unico(tipo(TipoSanguineo.A_POSITIVO, 100, 100, 10, 0)).situacao()).isEqualTo("ESTAVEL");
    }

    @Test
    @DisplayName("situação: ATENCAO abaixo de 70% da meta e acima do limiar crítico")
    void atencao() {
        assertThat(unico(tipo(TipoSanguineo.A_POSITIVO, 69, 100, 10, 0)).situacao()).isEqualTo("ATENCAO");
        assertThat(unico(tipo(TipoSanguineo.A_POSITIVO, 11, 100, 10, 0)).situacao()).isEqualTo("ATENCAO");
    }

    @Test
    @DisplayName("situação: CRITICO no limiar ou abaixo dele, mesmo que o percentual seja alto")
    void critico() {
        ResumoEstoque.PorTipo noLimiar = unico(tipo(TipoSanguineo.O_NEGATIVO, 10, 100, 10, 0));
        assertThat(noLimiar.situacao()).isEqualTo("CRITICO");
        assertThat(noLimiar.critico()).isTrue();
        assertThat(unico(tipo(TipoSanguineo.O_NEGATIVO, 0, 100, 10, 0)).situacao()).isEqualTo("CRITICO");
        assertThat(unico(tipo(TipoSanguineo.O_NEGATIVO, 90, 100, 95, 0)).situacao()).isEqualTo("CRITICO");
    }

    @Test
    @DisplayName("percentual: arredonda, limita a 100 e vira 0 quando a meta é zero")
    void percentual() {
        assertThat(unico(tipo(TipoSanguineo.B_POSITIVO, 1, 3, 0, 0)).percentual()).isEqualTo(33);
        assertThat(unico(tipo(TipoSanguineo.B_POSITIVO, 2, 3, 0, 0)).percentual()).isEqualTo(67);
        assertThat(unico(tipo(TipoSanguineo.B_POSITIVO, 500, 100, 0, 0)).percentual()).isEqualTo(100);
        assertThat(unico(tipo(TipoSanguineo.B_POSITIVO, 50, 0, 0, 0)).percentual()).isEqualTo(0);
    }

    @Test
    @DisplayName("meta zero com unidades acima do limiar: percentual 0 resulta em ATENCAO")
    void metaZero() {
        assertThat(unico(tipo(TipoSanguineo.B_POSITIVO, 50, 0, 0, 0)).situacao()).isEqualTo("ATENCAO");
    }

    @Test
    @DisplayName("porTipo sai na ordem do enum, com tipo, rótulo e dados de cada um")
    void ordenacaoEDados() {
        ResumoEstoque r = ResumoEstoque.de(List.of(
                tipo(TipoSanguineo.O_NEGATIVO, 5, 50, 10, 1),
                tipo(TipoSanguineo.A_POSITIVO, 40, 50, 10, 2)));

        assertThat(r.porTipo()).extracting(ResumoEstoque.PorTipo::tipoSanguineo)
                .containsExactly("A_POSITIVO", "O_NEGATIVO");
        assertThat(r.porTipo()).extracting(ResumoEstoque.PorTipo::rotulo).containsExactly("A+", "O-");
        assertThat(r.porTipo().get(0).vencendoEm7Dias()).isEqualTo(2);
        assertThat(r.porTipo().get(0).limiarCritico()).isEqualTo(10);
    }

    @Test
    @DisplayName("geral soma unidades, meta e vencimentos; lista os tipos críticos")
    void totais() {
        ResumoEstoque r = ResumoEstoque.de(List.of(
                tipo(TipoSanguineo.A_POSITIVO, 40, 50, 10, 2),
                tipo(TipoSanguineo.O_NEGATIVO, 5, 50, 10, 1),
                tipo(TipoSanguineo.AB_NEGATIVO, 8, 20, 10, 0)));

        assertThat(r.geral().unidades()).isEqualTo(53);
        assertThat(r.geral().meta()).isEqualTo(120);
        assertThat(r.geral().percentual()).isEqualTo(44);
        assertThat(r.geral().vencendoEm7Dias()).isEqualTo(3);
        assertThat(r.geral().criticos()).isEqualTo(2);
        assertThat(r.tiposCriticos()).containsExactly("AB-", "O-");
    }

    @Test
    @DisplayName("lista vazia gera resumo zerado")
    void vazio() {
        ResumoEstoque r = ResumoEstoque.de(List.of());

        assertThat(r.porTipo()).isEmpty();
        assertThat(r.tiposCriticos()).isEmpty();
        assertThat(r.geral().unidades()).isZero();
        assertThat(r.geral().percentual()).isZero();
    }
}
