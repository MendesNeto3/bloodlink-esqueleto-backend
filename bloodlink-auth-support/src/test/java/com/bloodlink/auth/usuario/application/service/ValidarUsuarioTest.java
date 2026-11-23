package com.bloodlink.auth.usuario.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidarUsuarioTest {

    @Test
    @DisplayName("e-mail: apara, passa para minúsculas e valida o formato")
    void email() {
        assertThat(ValidarUsuario.validarEmail("  Ana.Souza@Centro.COM ")).isEqualTo("ana.souza@centro.com");
        for (String ruim : new String[]{null, "", "  ", "semarroba", "a@b", "a b@c.com", "a".repeat(115) + "@x.com"}) {
            assertThatThrownBy(() -> ValidarUsuario.validarEmail(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("nome: 3 a 120 caracteres, espaços normalizados")
    void nome() {
        assertThat(ValidarUsuario.validarNome("  Ana   Souza ")).isEqualTo("Ana Souza");
        for (String ruim : new String[]{null, "  ", "Al", "x".repeat(121)}) {
            assertThatThrownBy(() -> ValidarUsuario.validarNome(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("senha: mínimo 8 caracteres, letras e números, máximo 72 bytes")
    void senha() {
        ValidarUsuario.validarSenha("abcdefg1");
        ValidarUsuario.validarSenha("a1" + "x".repeat(70));   // exatamente 72 bytes
        for (String ruim : new String[]{null, "", "abc123", "somenteletras", "12345678",
                "a1" + "x".repeat(71)}) {
            assertThatThrownBy(() -> ValidarUsuario.validarSenha(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("senha: o limite é em bytes, então acentos contam mais de um byte")
    void senhaEmBytes() {
        // 36 caracteres de 2 bytes + 1 letra + 1 dígito = 74 bytes
        assertThatThrownBy(() -> ValidarUsuario.validarSenha("ç".repeat(36) + "a1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("centroId: opcional; aceita 12 hexadecimais ou UUID; rejeita o resto")
    void centroId() {
        assertThat(ValidarUsuario.validarCentroId(null)).isNull();
        assertThat(ValidarUsuario.validarCentroId("  ")).isNull();
        assertThat(ValidarUsuario.validarCentroId(" 95B82D103312 ")).isEqualTo("95b82d103312");
        assertThat(ValidarUsuario.validarCentroId("95b82d10-3312-4a5b-8c9d-0123456789ab")).hasSize(36);
        for (String ruim : new String[]{"xyz", "95b82d10331", "../public", "95b82d103312; drop"}) {
            assertThatThrownBy(() -> ValidarUsuario.validarCentroId(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
