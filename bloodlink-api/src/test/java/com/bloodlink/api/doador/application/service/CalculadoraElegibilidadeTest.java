package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.out.Impedimento;
import com.bloodlink.api.doador.application.port.out.ResultadoElegibilidade;
import com.bloodlink.api.doador.application.service.CalculadoraElegibilidade.Entrada;
import com.bloodlink.api.doador.domain.MotivoInelegibilidade;
import com.bloodlink.api.doador.domain.StatusElegibilidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraElegibilidadeTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);
    private static final LocalDate NASCIMENTO_ADULTO = LocalDate.of(1990, 5, 10);   // 36 anos
    private static final BigDecimal PESO_OK = new BigDecimal("70");

    private static Entrada homem(LocalDate nascimento, BigDecimal peso, List<Impedimento> imp, List<LocalDate> doacoes) {
        return new Entrada(nascimento, peso, 60, 4, imp, doacoes);
    }

    private static Entrada mulher(LocalDate nascimento, BigDecimal peso, List<Impedimento> imp, List<LocalDate> doacoes) {
        return new Entrada(nascimento, peso, 90, 3, imp, doacoes);
    }

    private static Impedimento temporario(LocalDate inicio, LocalDate fim) {
        return new Impedimento(UUID.randomUUID(), UUID.randomUUID(), false, inicio, fim, "motivo",
                UUID.randomUUID(), Instant.EPOCH, null);
    }

    private static Impedimento definitivo(LocalDate inicio) {
        return new Impedimento(UUID.randomUUID(), UUID.randomUUID(), true, inicio, null, "motivo",
                UUID.randomUUID(), Instant.EPOCH, null);
    }

    // ---------- caso feliz e idade ----------

    @Test
    @DisplayName("adulto, peso adequado, sem doações nem impedimentos: elegível")
    void elegivel() {
        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK, null, null), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.ELEGIVEL);
        assertThat(r.motivos()).isEmpty();
        assertThat(r.elegivelEm()).isNull();
    }

    @Test
    @DisplayName("idade: 16 anos entra, 15 não; 69 entra, 70 não")
    void faixaDeIdade() {
        LocalDate fazDezesseis = HOJE.minusYears(16);
        LocalDate quase = HOJE.minusYears(16).plusDays(1);
        LocalDate sessentaENove = HOJE.minusYears(69);
        LocalDate setenta = HOJE.minusYears(70);
        List<LocalDate> doouJovem = List.of(HOJE.minusYears(30));

        assertThat(CalculadoraElegibilidade.avaliar(homem(fazDezesseis, PESO_OK, null, null), HOJE).status())
                .isEqualTo(StatusElegibilidade.ELEGIVEL);
        ResultadoElegibilidade menor = CalculadoraElegibilidade.avaliar(homem(quase, PESO_OK, null, null), HOJE);
        assertThat(menor.status()).isEqualTo(StatusElegibilidade.IMPEDIDO);
        assertThat(menor.motivos()).containsExactly(MotivoInelegibilidade.IDADE_FORA_DA_FAIXA);
        assertThat(menor.elegivelEm()).isNull();
        assertThat(CalculadoraElegibilidade.avaliar(homem(sessentaENove, PESO_OK, null, doouJovem), HOJE).status())
                .isEqualTo(StatusElegibilidade.ELEGIVEL);
        assertThat(CalculadoraElegibilidade.avaliar(homem(setenta, PESO_OK, null, doouJovem), HOJE).motivos())
                .containsExactly(MotivoInelegibilidade.IDADE_FORA_DA_FAIXA);
    }

    @Test
    @DisplayName("60 anos ou mais só doa se já tinha doado antes dos 60")
    void primeiraDoacaoApos60() {
        LocalDate nascimento = LocalDate.of(1960, 1, 1);   // 66 anos; fez 60 em 2020-01-01

        ResultadoElegibilidade nunca = CalculadoraElegibilidade.avaliar(homem(nascimento, PESO_OK, null, null), HOJE);
        assertThat(nunca.status()).isEqualTo(StatusElegibilidade.IMPEDIDO);
        assertThat(nunca.motivos()).containsExactly(MotivoInelegibilidade.PRIMEIRA_DOACAO_APOS_60_ANOS);

        ResultadoElegibilidade soDepois = CalculadoraElegibilidade.avaliar(
                homem(nascimento, PESO_OK, null, List.of(LocalDate.of(2021, 3, 1))), HOJE);
        assertThat(soDepois.motivos()).containsExactly(MotivoInelegibilidade.PRIMEIRA_DOACAO_APOS_60_ANOS);

        ResultadoElegibilidade jaDoava = CalculadoraElegibilidade.avaliar(
                homem(nascimento, PESO_OK, null, List.of(LocalDate.of(2015, 1, 1))), HOJE);
        assertThat(jaDoava.status()).isEqualTo(StatusElegibilidade.ELEGIVEL);
    }

    // ---------- peso ----------

    @Test
    @DisplayName("peso: 50 kg pode, 49,9 kg não, ausente aguarda a informação")
    void peso() {
        assertThat(CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, new BigDecimal("50"), null, null), HOJE).status())
                .isEqualTo(StatusElegibilidade.ELEGIVEL);

        ResultadoElegibilidade leve = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, new BigDecimal("49.9"), null, null), HOJE);
        assertThat(leve.status()).isEqualTo(StatusElegibilidade.IMPEDIDO);
        assertThat(leve.motivos()).containsExactly(MotivoInelegibilidade.PESO_INSUFICIENTE);
        assertThat(leve.elegivelEm()).isNull();

        ResultadoElegibilidade semPeso = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, null, null, null), HOJE);
        assertThat(semPeso.status()).isEqualTo(StatusElegibilidade.AGUARDANDO);
        assertThat(semPeso.motivos()).containsExactly(MotivoInelegibilidade.PESO_NAO_INFORMADO);
    }

    // ---------- impedimentos ----------

    @Test
    @DisplayName("impedimento definitivo vigente impede, sem previsão de retorno")
    void impedimentoDefinitivo() {
        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK, List.of(definitivo(HOJE.minusDays(10))), null), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.IMPEDIDO);
        assertThat(r.motivos()).containsExactly(MotivoInelegibilidade.IMPEDIMENTO_DEFINITIVO);
        assertThat(r.elegivelEm()).isNull();
    }

    @Test
    @DisplayName("impedimento temporário impede e prevê o retorno no dia seguinte ao fim")
    void impedimentoTemporario() {
        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK,
                        List.of(temporario(HOJE.minusDays(5), LocalDate.of(2026, 12, 1))), null), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.IMPEDIDO);
        assertThat(r.motivos()).containsExactly(MotivoInelegibilidade.IMPEDIMENTO_TEMPORARIO);
        assertThat(r.elegivelEm()).isEqualTo(LocalDate.of(2026, 12, 2));
    }

    @Test
    @DisplayName("vários temporários: vale o que termina por último")
    void variosTemporarios() {
        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK, List.of(
                        temporario(HOJE.minusDays(5), LocalDate.of(2026, 11, 30)),
                        temporario(HOJE.minusDays(2), LocalDate.of(2027, 1, 15))), null), HOJE);

        assertThat(r.elegivelEm()).isEqualTo(LocalDate.of(2027, 1, 16));
        assertThat(r.motivos()).containsExactly(MotivoInelegibilidade.IMPEDIMENTO_TEMPORARIO);
    }

    @Test
    @DisplayName("temporário junto de outro motivo permanente não promete data de retorno")
    void temporarioMaisPesoInsuficiente() {
        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                mulher(NASCIMENTO_ADULTO, new BigDecimal("45"),
                        List.of(temporario(HOJE.minusDays(1), HOJE.plusDays(10))), null), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.IMPEDIDO);
        assertThat(r.motivos()).containsExactlyInAnyOrder(
                MotivoInelegibilidade.PESO_INSUFICIENTE, MotivoInelegibilidade.IMPEDIMENTO_TEMPORARIO);
        assertThat(r.elegivelEm()).isNull();
    }

    @Test
    @DisplayName("impedimento vencido, futuro ou encerrado pela equipe não conta")
    void impedimentosQueNaoValem() {
        Impedimento vencido = temporario(HOJE.minusDays(30), HOJE.minusDays(1));
        Impedimento futuro = temporario(HOJE.plusDays(5), HOJE.plusDays(20));
        Impedimento encerrado = definitivo(HOJE.minusDays(10)).encerrado(Instant.parse("2026-11-10T10:00:00Z"));

        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK, List.of(vencido, futuro, encerrado), null), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.ELEGIVEL);
    }

    @Test
    @DisplayName("temporário que termina hoje ainda vale; no dia seguinte já não")
    void fimDoTemporarioEInclusivo() {
        Impedimento t = temporario(HOJE.minusDays(10), HOJE);

        assertThat(CalculadoraElegibilidade.avaliar(homem(NASCIMENTO_ADULTO, PESO_OK, List.of(t), null), HOJE)
                .status()).isEqualTo(StatusElegibilidade.IMPEDIDO);
        assertThat(CalculadoraElegibilidade.avaliar(homem(NASCIMENTO_ADULTO, PESO_OK, List.of(t), null),
                HOJE.plusDays(1)).status()).isEqualTo(StatusElegibilidade.ELEGIVEL);
    }

    // ---------- intervalo e limite anual ----------

    @Test
    @DisplayName("homem: dentro de 60 dias da última doação aguarda, e informa a data")
    void intervaloMinimo() {
        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK, null, List.of(LocalDate.of(2026, 10, 1))), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.AGUARDANDO);
        assertThat(r.motivos()).containsExactly(MotivoInelegibilidade.INTERVALO_MINIMO);
        assertThat(r.elegivelEm()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    @DisplayName("no dia exato do intervalo (60 dias depois) já está elegível")
    void intervaloNoDiaExato() {
        LocalDate ultima = HOJE.minusDays(60);

        assertThat(CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK, null, List.of(ultima)), HOJE).status())
                .isEqualTo(StatusElegibilidade.ELEGIVEL);
        assertThat(CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK, null, List.of(ultima.plusDays(1))), HOJE).status())
                .isEqualTo(StatusElegibilidade.AGUARDANDO);
    }

    @Test
    @DisplayName("mulher: o intervalo é de 90 dias")
    void intervaloDaMulher() {
        List<LocalDate> doacoes = List.of(HOJE.minusDays(70));

        assertThat(CalculadoraElegibilidade.avaliar(mulher(NASCIMENTO_ADULTO, PESO_OK, null, doacoes), HOJE)
                .status()).isEqualTo(StatusElegibilidade.AGUARDANDO);
        assertThat(CalculadoraElegibilidade.avaliar(homem(NASCIMENTO_ADULTO, PESO_OK, null, doacoes), HOJE)
                .status()).isEqualTo(StatusElegibilidade.ELEGIVEL);
    }

    @Test
    @DisplayName("limite anual: mulher com 3 doações em 12 meses aguarda até a mais antiga sair da janela")
    void limiteAnual() {
        List<LocalDate> doacoes = List.of(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 7, 20));

        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                mulher(NASCIMENTO_ADULTO, PESO_OK, null, doacoes), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.AGUARDANDO);
        assertThat(r.motivos()).containsExactly(MotivoInelegibilidade.LIMITE_ANUAL);
        assertThat(r.elegivelEm()).isEqualTo(LocalDate.of(2027, 1, 10));
    }

    @Test
    @DisplayName("intervalo e limite anual juntos: vale a data mais distante")
    void intervaloELimiteJuntos() {
        // homem, limite 4; última doação há 10 dias (intervalo) e 4 doações na janela (limite)
        List<LocalDate> doacoes = List.of(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 8, 1), HOJE.minusDays(10));

        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK, null, doacoes), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.AGUARDANDO);
        assertThat(r.motivos()).containsExactlyInAnyOrder(
                MotivoInelegibilidade.INTERVALO_MINIMO, MotivoInelegibilidade.LIMITE_ANUAL);
        assertThat(r.elegivelEm()).isEqualTo(LocalDate.of(2027, 2, 1));
    }

    @Test
    @DisplayName("doação com mais de 12 meses não entra no limite anual")
    void doacaoForaDaJanela() {
        List<LocalDate> doacoes = List.of(HOJE.minusYears(1), HOJE.minusDays(300), HOJE.minusDays(200));

        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                mulher(NASCIMENTO_ADULTO, PESO_OK, null, doacoes), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.ELEGIVEL);
    }

    @Test
    @DisplayName("doações com data futura são ignoradas")
    void ignoraDoacoesFuturas() {
        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK, null, List.of(HOJE.plusDays(3))), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.ELEGIVEL);
    }

    @Test
    @DisplayName("impedimento vence a espera: quem está impedido não aparece como 'aguardando'")
    void impedimentoPrevalece() {
        ResultadoElegibilidade r = CalculadoraElegibilidade.avaliar(
                homem(NASCIMENTO_ADULTO, PESO_OK, List.of(definitivo(HOJE.minusDays(1))),
                        List.of(HOJE.minusDays(5))), HOJE);

        assertThat(r.status()).isEqualTo(StatusElegibilidade.IMPEDIDO);
    }

    // ---------- Entrada ----------

    @Test
    @DisplayName("Entrada exige nascimento, intervalo e limite positivos")
    void entradaInvalida() {
        assertThatThrownBy(() -> new Entrada(null, PESO_OK, 60, 4, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Entrada(NASCIMENTO_ADULTO, PESO_OK, 0, 4, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Entrada(NASCIMENTO_ADULTO, PESO_OK, 60, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Impedimento valida as datas e o tipo")
    void impedimentoInvalido() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new Impedimento(id, id, false, HOJE, null, "m", id, Instant.EPOCH, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Impedimento(id, id, true, HOJE, HOJE, "m", id, Instant.EPOCH, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Impedimento(id, id, false, HOJE, HOJE.minusDays(1), "m", id, Instant.EPOCH, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Impedimento(id, id, false, null, HOJE, "m", id, Instant.EPOCH, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Impedimento.toString não expõe o motivo (dado de saúde)")
    void impedimentoToString() {
        Impedimento i = new Impedimento(UUID.randomUUID(), UUID.randomUUID(), true, HOJE, null,
                "hepatite B", UUID.randomUUID(), Instant.EPOCH, null);

        assertThat(i.toString()).doesNotContain("hepatite");
    }
}
