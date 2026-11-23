package com.bloodlink.common.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class MascaraLogTest {

    @Test
    @DisplayName("uri: esconde cpf, e-mail e nome no caminho e na query")
    void uri() {
        assertThat(MascaraLog.uri("/api/doadores/cpf/52998224725")).isEqualTo("/api/doadores/cpf/***");
        assertThat(MascaraLog.uri("/api/doadores/email/a@b.com/x")).isEqualTo("/api/doadores/email/***/x");
        assertThat(MascaraLog.uri("/api/doadores?email=a@b.com&pagina=1")).isEqualTo("/api/doadores?email=***&pagina=1");
        assertThat(MascaraLog.uri("/api/doadores?NOME=Maria&cpf=123")).isEqualTo("/api/doadores?NOME=***&cpf=***");
        assertThat(MascaraLog.uri("/api/painel")).isEqualTo("/api/painel");
        assertThat(MascaraLog.uri(null)).isNull();
    }

    @Test
    @DisplayName("cpf: mantém só os 3 primeiros e os 2 últimos dígitos")
    void cpf() {
        assertThat(MascaraLog.cpf("52998224725")).isEqualTo("529.***.***-25");
        assertThat(MascaraLog.cpf("529.982.247-25")).isEqualTo("529.***.***-25");
        assertThat(MascaraLog.cpf("123")).isEqualTo("***");
        assertThat(MascaraLog.cpf(null)).isNull();
    }

    @Test
    @DisplayName("email: primeira letra e domínio")
    void email() {
        assertThat(MascaraLog.email("maria@x.com")).isEqualTo("m***@x.com");
        assertThat(MascaraLog.email("@x.com")).isEqualTo("***");
        assertThat(MascaraLog.email("semarroba")).isEqualTo("***");
        assertThat(MascaraLog.email(null)).isNull();
        assertThat(MascaraLog.email("  ")).isEqualTo("  ");
    }

    @Test
    @DisplayName("nome: só iniciais")
    void nome() {
        assertThat(MascaraLog.nome("Maria da Silva")).isEqualTo("M. d. S.");
        assertThat(MascaraLog.nome("  Ana   Souza ")).isEqualTo("A. S.");
        assertThat(MascaraLog.nome(null)).isNull();
        assertThat(MascaraLog.nome(" ")).isEqualTo(" ");
    }

    @Test
    @DisplayName("texto livre: mascara cpf e e-mail em qualquer lugar da frase")
    void texto() {
        assertThat(MascaraLog.texto("Doador 529.982.247-25 contato a@b.com falhou"))
                .isEqualTo("Doador 529.***.***-25 contato ***@*** falhou");
        assertThat(MascaraLog.texto("52998224725")).isEqualTo("529.***.***-25");
        assertThat(MascaraLog.texto("sem dados pessoais")).isEqualTo("sem dados pessoais");
        assertThat(MascaraLog.texto(null)).isNull();
    }

    @Test
    @DisplayName("causa: devolve só a classe da causa raiz, sem mensagem")
    void causa() {
        Throwable t = new RuntimeException("dados do cpf 52998224725",
                new IllegalStateException("segredo"));

        assertThat(MascaraLog.causa(t)).isEqualTo("IllegalStateException");
        assertThat(MascaraLog.causa(new RuntimeException("x"))).isEqualTo("RuntimeException");
        assertThat(MascaraLog.causa(null)).isEqualTo("desconhecida");
    }

    @Test
    @DisplayName("causa: inclui o sqlState quando a raiz é SQLException")
    void causaSql() {
        Throwable t = new RuntimeException(new SQLException("duplicate key cpf=1", "23505"));

        assertThat(MascaraLog.causa(t)).isEqualTo("SQLException sqlState=23505");
    }
}
