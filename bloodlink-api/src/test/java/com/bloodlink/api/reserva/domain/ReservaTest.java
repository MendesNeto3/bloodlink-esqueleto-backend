package com.bloodlink.api.reserva.domain;

import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);
    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");

    private Reserva reservaValidaAte(LocalDate validaAte) {
        return Reserva.nova("Hospital Central", TipoSanguineo.O_NEGATIVO, 4, validaAte, UUID.randomUUID(), AGORA);
    }

    @Test
    @DisplayName("nova reserva nasce ativa e sem data de liberação")
    void novaNasceAtiva() {
        Reserva r = reservaValidaAte(HOJE.plusDays(5));

        assertThat(r.status()).isEqualTo(StatusReserva.ATIVA);
        assertThat(r.liberadaEm()).isNull();
        assertThat(r.criadoEm()).isEqualTo(AGORA);
        assertThat(r.situacao(HOJE)).isEqualTo(SituacaoReserva.ATIVA);
    }

    @Test
    @DisplayName("vale até o fim do dia de validade e vence no dia seguinte")
    void validadeVaiAteOFimDoDia() {
        Reserva r = reservaValidaAte(HOJE);

        assertThat(r.situacao(HOJE)).isEqualTo(SituacaoReserva.ATIVA);
        assertThat(r.situacao(HOJE.plusDays(1))).isEqualTo(SituacaoReserva.VENCIDA);
    }

    @Test
    @DisplayName("liberar marca como liberada e guarda o instante")
    void liberarMarcaLiberada() {
        Reserva r = reservaValidaAte(HOJE.plusDays(5));
        Instant depois = AGORA.plusSeconds(60);

        Reserva liberada = r.liberar(HOJE, depois);

        assertThat(liberada.status()).isEqualTo(StatusReserva.LIBERADA);
        assertThat(liberada.liberadaEm()).isEqualTo(depois);
        assertThat(liberada.id()).isEqualTo(r.id());
        assertThat(liberada.situacao(HOJE)).isEqualTo(SituacaoReserva.LIBERADA);
    }

    @Test
    @DisplayName("reserva liberada continua liberada mesmo depois da validade")
    void liberadaNaoViraVencida() {
        Reserva liberada = reservaValidaAte(HOJE).liberar(HOJE, AGORA);

        assertThat(liberada.situacao(HOJE.plusDays(30))).isEqualTo(SituacaoReserva.LIBERADA);
    }

    @Test
    @DisplayName("não libera reserva já liberada")
    void naoLiberaDuasVezes() {
        Reserva liberada = reservaValidaAte(HOJE.plusDays(5)).liberar(HOJE, AGORA);

        assertThatThrownBy(() -> liberada.liberar(HOJE, AGORA))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("não libera reserva vencida")
    void naoLiberaVencida() {
        Reserva vencida = reservaValidaAte(HOJE.minusDays(1));

        assertThatThrownBy(() -> vencida.liberar(HOJE, AGORA))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("código tem prefixo RS- e 8 caracteres hexadecimais maiúsculos do id")
    void codigoDerivaDoId() {
        Reserva r = reservaValidaAte(HOJE.plusDays(5));

        assertThat(r.codigo()).matches("RS-[0-9A-F]{8}");
        assertThat(r.codigo()).isEqualTo("RS-" + r.id().toString().substring(0, 8).toUpperCase());
    }

    @Test
    @DisplayName("toString mostra só o id, sem hospital nem dados da reserva")
    void toStringNaoVazaDados() {
        Reserva r = reservaValidaAte(HOJE.plusDays(5));

        assertThat(r.toString()).contains(r.id().toString()).doesNotContain("Hospital Central");
    }
}
