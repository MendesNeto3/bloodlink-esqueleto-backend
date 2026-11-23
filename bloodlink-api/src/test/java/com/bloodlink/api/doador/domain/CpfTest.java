package com.bloodlink.api.doador.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfTest {

    @Test
    @DisplayName("normalizar tira tudo que não é dígito e preserva nulo")
    void normalizar() {
        assertThat(Cpf.normalizar("529.982.247-25")).isEqualTo("52998224725");
        assertThat(Cpf.normalizar(" 529 982 247 25 ")).isEqualTo("52998224725");
        assertThat(Cpf.normalizar(null)).isNull();
    }

    @Test
    @DisplayName("aceita CPFs com dígitos verificadores corretos")
    void validos() {
        assertThat(Cpf.valido("52998224725")).isTrue();
        assertThat(Cpf.valido("11144477735")).isTrue();
    }

    @Test
    @DisplayName("rejeita dígito verificador errado")
    void digitoErrado() {
        assertThat(Cpf.valido("52998224726")).isFalse();
        assertThat(Cpf.valido("52998224715")).isFalse();
    }

    @Test
    @DisplayName("rejeita sequências repetidas, tamanho errado e nulo")
    void invalidos() {
        assertThat(Cpf.valido("11111111111")).isFalse();
        assertThat(Cpf.valido("00000000000")).isFalse();
        assertThat(Cpf.valido("5299822472")).isFalse();
        assertThat(Cpf.valido("529982247250")).isFalse();
        assertThat(Cpf.valido("")).isFalse();
        assertThat(Cpf.valido(null)).isFalse();
    }
}
