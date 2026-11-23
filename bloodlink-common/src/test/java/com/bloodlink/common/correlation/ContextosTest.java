package com.bloodlink.common.correlation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContextosTest {

    @AfterEach
    void limpar() {
        CorrelationContext.clear();
        UserRoleContext.clear();
    }

    @Test
    @DisplayName("correlation id: usa o informado, gera um UUID quando nulo e limpa")
    void correlation() {
        CorrelationContext.set("abc-123");
        assertThat(CorrelationContext.get()).isEqualTo("abc-123");

        CorrelationContext.set(null);
        assertThat(UUID.fromString(CorrelationContext.get())).isNotNull();

        CorrelationContext.clear();
        String semContexto = CorrelationContext.get();
        assertThat(UUID.fromString(semContexto)).isNotNull();
        assertThat(CorrelationContext.gerar()).isNotEqualTo(CorrelationContext.gerar());
        assertThat(CorrelationContext.HEADER).isEqualTo("X-Correlation-Id");
    }

    @Test
    @DisplayName("papel do usuário: guarda, devolve e limpa")
    void papel() {
        assertThat(UserRoleContext.get()).isNull();
        UserRoleContext.set("ADMIN_CENTRO");
        assertThat(UserRoleContext.get()).isEqualTo("ADMIN_CENTRO");
        UserRoleContext.clear();
        assertThat(UserRoleContext.get()).isNull();
        assertThat(UserRoleContext.HEADER).isEqualTo("X-User-Role");
    }
}
