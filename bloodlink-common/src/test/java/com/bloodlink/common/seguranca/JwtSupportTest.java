package com.bloodlink.common.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSupportTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!!";
    private static final String OUTRO = "outro-segredo-de-teste-com-32-caracteres-ou-mais";

    private final JwtSupport jwt = new JwtSupport(SEGREDO, Duration.ofMinutes(15));

    @Test
    @DisplayName("token gerado é aceito e devolve subject, centro, papel e doador")
    void ciclo() {
        String token = jwt.gerarToken("user-1", "95b82d103312", "DOADOR", "doador-1");

        Optional<JwtSupport.DadosToken> dados = jwt.validar(token);

        assertThat(dados).isPresent();
        assertThat(dados.get().subject()).isEqualTo("user-1");
        assertThat(dados.get().centroId()).isEqualTo("95b82d103312");
        assertThat(dados.get().papel()).isEqualTo("DOADOR");
        assertThat(dados.get().doadorId()).isEqualTo("doador-1");
    }

    @Test
    @DisplayName("token da plataforma não tem centro nem doador")
    void plataforma() {
        JwtSupport.DadosToken d = jwt.validar(jwt.gerarToken("admin", null, "ADMIN_PLATAFORMA")).orElseThrow();

        assertThat(d.centroId()).isNull();
        assertThat(d.doadorId()).isNull();
    }

    @Test
    @DisplayName("tokens de equipe e de doador têm ids diferentes a cada geração")
    void tokensDiferentes() {
        assertThat(jwt.gerarToken("u", "95b82d103312", "RECEPCAO"))
                .isNotEqualTo(jwt.gerarToken("u", "95b82d103312", "RECEPCAO"));
    }

    @Test
    @DisplayName("token assinado com outro segredo é recusado")
    void forjado() {
        String forjado = new JwtSupport(OUTRO, Duration.ofMinutes(15)).gerarToken("u", "95b82d103312", "ADMIN_CENTRO");

        assertThat(jwt.validar(forjado)).isEmpty();
    }

    @Test
    @DisplayName("token expirado (além da tolerância de 30s) é recusado")
    void expirado() {
        String velho = new JwtSupport(SEGREDO, Duration.ofSeconds(-120)).gerarToken("u", "95b82d103312", "ADMIN_CENTRO");

        assertThat(jwt.validar(velho)).isEmpty();
    }

    @Test
    @DisplayName("lixo, nulo e em branco são recusados sem lançar exceção")
    void lixo() {
        assertThat(jwt.validar(null)).isEmpty();
        assertThat(jwt.validar("")).isEmpty();
        assertThat(jwt.validar("   ")).isEmpty();
        assertThat(jwt.validar("nao.e.jwt")).isEmpty();
        assertThat(jwt.validar("abc")).isEmpty();
    }

    @Test
    @DisplayName("token adulterado no payload é recusado")
    void adulterado() {
        String token = jwt.gerarToken("u", "95b82d103312", "RECEPCAO");
        String[] partes = token.split("\\.");
        String adulterado = partes[0] + "." + partes[1] + "x." + partes[2];

        assertThat(jwt.validar(adulterado)).isEmpty();
    }

    @Test
    @DisplayName("segredo curto ou ausente impede a criação")
    void segredoCurto() {
        assertThatThrownBy(() -> new JwtSupport("curto", Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtSupport(null, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("subject e papel são obrigatórios na geração")
    void obrigatorios() {
        assertThatThrownBy(() -> jwt.gerarToken(null, "c", "DOADOR")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwt.gerarToken(" ", "c", "DOADOR")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwt.gerarToken("u", "c", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwt.gerarToken("u", "c", " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
