package com.bloodlink.api.doador.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacoesDoadorTest {

    private static LocalDate hoje() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    }

    @Test
    @DisplayName("nome: apara, junta espaços e respeita 3 a 120 caracteres")
    void nome() {
        assertThat(ValidacoesDoador.validarNome("  Maria   da  Silva ")).isEqualTo("Maria da Silva");
        assertThat(ValidacoesDoador.validarNome("Ana")).isEqualTo("Ana");
        for (String ruim : new String[]{null, "", "   ", "Al", "x".repeat(121)}) {
            assertThatThrownBy(() -> ValidacoesDoador.validarNome(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("UF: opcional, normaliza para maiúsculas e rejeita sigla inexistente")
    void uf() {
        assertThat(ValidacoesDoador.validarUf(null)).isNull();
        assertThat(ValidacoesDoador.validarUf("  ")).isNull();
        assertThat(ValidacoesDoador.validarUf(" sp ")).isEqualTo("SP");
        assertThatThrownBy(() -> ValidacoesDoador.validarUf("XX")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidacoesDoador.validarUf("SPP")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("cidade: opcional, 2 a 80 caracteres")
    void cidade() {
        assertThat(ValidacoesDoador.validarCidade(null)).isNull();
        assertThat(ValidacoesDoador.validarCidade("  ")).isNull();
        assertThat(ValidacoesDoador.validarCidade("  São   Paulo ")).isEqualTo("São Paulo");
        assertThatThrownBy(() -> ValidacoesDoador.validarCidade("A")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidacoesDoador.validarCidade("x".repeat(81)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CPF: aceita com máscara e devolve só dígitos; rejeita inválidos")
    void cpf() {
        assertThat(ValidacoesDoador.validarCpf("529.982.247-25")).isEqualTo("52998224725");
        for (String ruim : new String[]{null, "", "  ", "111.111.111-11", "529.982.247-26", "123"}) {
            assertThatThrownBy(() -> ValidacoesDoador.validarCpf(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("nascimento: obrigatório, não pode ser futuro, mínimo 16 anos, máximo plausível 120")
    void nascimento() {
        LocalDate dezesseteAnos = hoje().minusYears(30);
        assertThat(ValidacoesDoador.validarNascimento(dezesseteAnos)).isEqualTo(dezesseteAnos);
        assertThat(ValidacoesDoador.validarNascimento(hoje().minusYears(16))).isNotNull();

        assertThatThrownBy(() -> ValidacoesDoador.validarNascimento(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidacoesDoador.validarNascimento(hoje().plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidacoesDoador.validarNascimento(hoje().minusYears(16).plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidacoesDoador.validarNascimento(hoje().minusYears(121)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("16 e 17 anos exigem consentimento do responsável; adultos não")
    void consentimento() {
        LocalDate adolescente = hoje().minusYears(17);
        LocalDate adulto = hoje().minusYears(30);

        assertThatThrownBy(() -> ValidacoesDoador.exigirConsentimentoSeAdolescente(adolescente, false))
                .isInstanceOf(IllegalArgumentException.class);
        ValidacoesDoador.exigirConsentimentoSeAdolescente(adolescente, true);
        ValidacoesDoador.exigirConsentimentoSeAdolescente(adulto, false);
    }

    @Test
    @DisplayName("peso: obrigatório, entre 20 e 300 kg, arredondado para uma casa")
    void peso() {
        assertThat(ValidacoesDoador.validarPeso(new BigDecimal("70.25"))).isEqualByComparingTo("70.3");
        assertThat(ValidacoesDoador.validarPeso(new BigDecimal("20"))).isEqualByComparingTo("20.0");
        assertThat(ValidacoesDoador.validarPeso(new BigDecimal("300"))).isEqualByComparingTo("300.0");
        for (BigDecimal ruim : new BigDecimal[]{null, new BigDecimal("19.9"), new BigDecimal("300.1")}) {
            assertThatThrownBy(() -> ValidacoesDoador.validarPeso(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("telefone: opcional, só dígitos, 10 ou 11 dígitos")
    void telefone() {
        assertThat(ValidacoesDoador.validarTelefone(null)).isNull();
        assertThat(ValidacoesDoador.validarTelefone(" ")).isNull();
        assertThat(ValidacoesDoador.validarTelefone("(11) 98765-4321")).isEqualTo("11987654321");
        assertThat(ValidacoesDoador.validarTelefone("(11) 3456-7890")).isEqualTo("1134567890");
        assertThatThrownBy(() -> ValidacoesDoador.validarTelefone("12345")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidacoesDoador.validarTelefone("123456789012"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("e-mail: opcional, minúsculas, formato válido e até 120 caracteres")
    void email() {
        assertThat(ValidacoesDoador.validarEmail(null)).isNull();
        assertThat(ValidacoesDoador.validarEmail("  ")).isNull();
        assertThat(ValidacoesDoador.validarEmail("  Maria.Silva@Exemplo.COM ")).isEqualTo("maria.silva@exemplo.com");
        for (String ruim : new String[]{"semarroba", "a@b", "a@@b.com", "a b@c.com", "@x.com",
                "a".repeat(115) + "@x.com"}) {
            assertThatThrownBy(() -> ValidacoesDoador.validarEmail(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
