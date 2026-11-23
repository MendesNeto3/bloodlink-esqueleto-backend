package com.bloodlink.common.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIdsTest {

    @Test
    @DisplayName("aceita 12 caracteres hexadecimais e normaliza para minúsculas")
    void valido() {
        assertThat(TenantIds.normalizar("95b82d103312")).contains("95b82d103312");
        assertThat(TenantIds.normalizar("  95B82D103312 ")).contains("95b82d103312");
        assertThat(TenantIds.ehValido("abcdef012345")).isTrue();
    }

    @Test
    @DisplayName("recusa nulo, vazio, tamanho errado, não hexadecimal e tentativa de injeção")
    void invalido() {
        for (String ruim : new String[]{null, "", "   ", "95b82d10331", "95b82d1033123", "95b82d10331g",
                "centro_95b82d103312", "95b82d103312; drop schema public", "../../95b82d1033"}) {
            assertThat(TenantIds.ehValido(ruim)).as(String.valueOf(ruim)).isFalse();
            assertThat(TenantIds.normalizar(ruim)).isEmpty();
        }
    }
}
