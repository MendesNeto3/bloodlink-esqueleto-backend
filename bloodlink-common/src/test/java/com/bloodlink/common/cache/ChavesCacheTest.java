package com.bloodlink.common.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChavesCacheTest {

    @Test
    @DisplayName("chave do nome do centro tem prefixo estável (API e Centro precisam concordar)")
    void nomeCentro() {
        assertThat(ChavesCache.nomeCentro("95b82d103312")).isEqualTo("bloodlink:centro:nome:95b82d103312");
    }
}
