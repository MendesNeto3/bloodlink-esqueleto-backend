package com.bloodlink.api.documento.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NomeArquivoTest {

    @Test
    @DisplayName("mantém um nome comum e usa a extensão do formato real")
    void nomeComum() {
        assertThat(NomeArquivo.seguro("exame de sangue.pdf", ".pdf")).isEqualTo("exame de sangue.pdf");
        assertThat(NomeArquivo.seguro("foto.exe", ".jpg")).isEqualTo("foto.jpg");
        assertThat(NomeArquivo.seguro("semextensao", ".png")).isEqualTo("semextensao.png");
    }

    @Test
    @DisplayName("remove caminhos (Unix e Windows)")
    void removeCaminhos() {
        assertThat(NomeArquivo.seguro("../../etc/passwd", ".pdf")).isEqualTo("passwd.pdf");
        assertThat(NomeArquivo.seguro("C:\\Users\\x\\exame.pdf", ".pdf")).isEqualTo("exame.pdf");
    }

    @Test
    @DisplayName("remove caracteres de controle e símbolos de arquivo")
    void removeSimbolos() {
        assertThat(NomeArquivo.seguro("a\"b*c:d<e>f?g|h.pdf", ".pdf")).isEqualTo("abcdefgh.pdf");
        assertThat(NomeArquivo.seguro("linha\nnova\t.pdf", ".pdf")).isEqualTo("linhanova.pdf");
    }

    @Test
    @DisplayName("junta espaços repetidos e apara as pontas")
    void normalizaEspacos() {
        assertThat(NomeArquivo.seguro("   meu    exame   .pdf", ".pdf")).isEqualTo("meu exame.pdf");
    }

    @Test
    @DisplayName("nome vazio, nulo ou só símbolos vira 'documento'")
    void nomeVazio() {
        assertThat(NomeArquivo.seguro(null, ".pdf")).isEqualTo("documento.pdf");
        assertThat(NomeArquivo.seguro("", ".pdf")).isEqualTo("documento.pdf");
        assertThat(NomeArquivo.seguro("???.pdf", ".pdf")).isEqualTo("documento.pdf");
        assertThat(NomeArquivo.seguro(".pdf", ".pdf")).isEqualTo("documento.pdf");
    }

    @Test
    @DisplayName("limita a 100 caracteres antes da extensão")
    void limitaTamanho() {
        String longo = "a".repeat(300) + ".pdf";
        String resultado = NomeArquivo.seguro(longo, ".pdf");

        assertThat(resultado).hasSize(100 + ".pdf".length()).endsWith(".pdf");
    }

    @Test
    @DisplayName("só a última extensão é trocada")
    void multiplasExtensoes() {
        assertThat(NomeArquivo.seguro("relatorio.final.docx", ".pdf")).isEqualTo("relatorio.final.pdf");
    }
}
