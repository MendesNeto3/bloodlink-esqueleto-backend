package com.bloodlink.centro.domain;

import com.bloodlink.centro.application.service.DistanciaGeografica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class DominioCentroTest {

    private PerfilCentro perfil(String cidade, String uf, BigDecimal lat, BigDecimal lng,
                                LocalTime abre, LocalTime fecha, Integer capacidade) {
        return new PerfilCentro(TipoCentro.FIXO, "Rua A, 10", cidade, uf, lat, lng,
                Set.of(DiaSemana.SEG, DiaSemana.TER), abre, fecha, capacidade);
    }

    @Test
    @DisplayName("perfil vazio: tipo FIXO, sem dias e sem dados")
    void vazio() {
        PerfilCentro p = PerfilCentro.vazio();

        assertThat(p.tipo()).isEqualTo(TipoCentro.FIXO);
        assertThat(p.diasFuncionamento()).isEmpty();
        assertThat(p.endereco()).isNull();
        assertThat(p.capacidadeDiaria()).isNull();
    }

    @Test
    @DisplayName("perfil: normaliza textos, UF em maiúsculas e coordenadas com 6 casas")
    void normaliza() {
        PerfilCentro p = new PerfilCentro(TipoCentro.MOVEL, "  Rua A  ", " Campinas ", " sp ",
                new BigDecimal("-22.9"), new BigDecimal("-47.06"), Set.of(DiaSemana.SAB),
                LocalTime.of(8, 0), LocalTime.of(17, 0), 50);

        assertThat(p.endereco()).isEqualTo("Rua A");
        assertThat(p.cidade()).isEqualTo("Campinas");
        assertThat(p.uf()).isEqualTo("SP");
        assertThat(p.latitude()).isEqualByComparingTo("-22.900000");
        assertThat(p.latitude().scale()).isEqualTo(6);
        assertThat(p.diasFuncionamento()).containsExactly(DiaSemana.SAB);
    }

    @Test
    @DisplayName("perfil: textos em branco viram nulo")
    void emBranco() {
        PerfilCentro p = new PerfilCentro(null, "  ", " ", " ", null, null, null, null, null, null);

        assertThat(p.endereco()).isNull();
        assertThat(p.cidade()).isNull();
        assertThat(p.uf()).isNull();
    }

    @Test
    @DisplayName("perfil: regras de pares (cidade/UF, lat/lng, abertura/fechamento)")
    void pares() {
        assertThatThrownBy(() -> perfil("Campinas", null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cidade e UF");
        assertThatThrownBy(() -> perfil(null, "SP", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> perfil(null, null, BigDecimal.ONE, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("latitude e longitude");
        assertThatThrownBy(() -> perfil(null, null, null, BigDecimal.ONE, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> perfil(null, null, null, null, LocalTime.of(8, 0), null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("abertura e fechamento");
    }

    @Test
    @DisplayName("perfil: limites de coordenada, horário, UF, capacidade e tamanho de texto")
    void limites() {
        assertThatThrownBy(() -> perfil(null, null, new BigDecimal("90.1"), BigDecimal.ZERO, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> perfil(null, null, BigDecimal.ZERO, new BigDecimal("-180.1"), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        perfil(null, null, new BigDecimal("90"), new BigDecimal("-180"), null, null, null);

        assertThatThrownBy(() -> perfil(null, null, null, null, LocalTime.of(17, 0), LocalTime.of(8, 0), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("antes do fechamento");
        assertThatThrownBy(() -> perfil(null, null, null, null, LocalTime.of(8, 0), LocalTime.of(8, 0), null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> perfil("Cidade", "XX", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("UF inválida");

        assertThatThrownBy(() -> perfil(null, null, null, null, null, null, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> perfil(null, null, null, null, null, null, 100_001)).isInstanceOf(IllegalArgumentException.class);
        perfil(null, null, null, null, null, null, 100_000);

        assertThatThrownBy(() -> new PerfilCentro(null, "x".repeat(201), null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PerfilCentro(null, null, "x".repeat(101), "SP", null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("comCapacidadeDiaria troca só a capacidade")
    void capacidade() {
        PerfilCentro p = perfil("Campinas", "SP", null, null, null, null, 10);

        PerfilCentro novo = p.comCapacidadeDiaria(99);

        assertThat(novo.capacidadeDiaria()).isEqualTo(99);
        assertThat(novo.cidade()).isEqualTo("Campinas");
        assertThat(p.capacidadeDiaria()).isEqualTo(10);
    }

    @Test
    @DisplayName("os dias de funcionamento não podem ser alterados por fora")
    void diasImutaveis() {
        PerfilCentro p = perfil(null, null, null, null, null, null, null);

        assertThatThrownBy(() -> p.diasFuncionamento().add(DiaSemana.DOM)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("centro: centroId é o que vem depois de 'centro_'; with* não alteram o original")
    void centro() {
        Centro c = new Centro(UUID.randomUUID(), "Hemocentro", "centro_95b82d103312", true);

        assertThat(c.centroId()).isEqualTo("95b82d103312");
        assertThat(c.perfil()).isEqualTo(PerfilCentro.vazio());
        assertThat(c.comNome("Novo").nome()).isEqualTo("Novo");
        assertThat(c.desativado().ativo()).isFalse();
        assertThat(c.ativo()).isTrue();
        assertThat(c.comPerfil(perfil("Campinas", "SP", null, null, null, null, 5)).perfil().cidade()).isEqualTo("Campinas");
        assertThat(new Centro(null, "X", "centro_aaaaaaaaaaaa", true, null).perfil()).isNotNull();
    }

    @Test
    @DisplayName("distância (Haversine): São Paulo–Rio ≈ 360 km; mesmo ponto = 0; simétrica")
    void distancia() {
        double sp = DistanciaGeografica.km(-23.5505, -46.6333, -22.9068, -43.1729);

        assertThat(sp).isCloseTo(360.0, within(5.0));
        assertThat(DistanciaGeografica.km(-23.5505, -46.6333, -23.5505, -46.6333)).isZero();
        assertThat(DistanciaGeografica.km(-22.9068, -43.1729, -23.5505, -46.6333)).isCloseTo(sp, within(0.001));
    }
}
