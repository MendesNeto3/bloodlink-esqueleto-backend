package com.bloodlink.api.documento.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormatoArquivoTest {

    static byte[] pdf() {
        return "%PDF-1.7 conteudo".getBytes();
    }

    static byte[] jpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    }

    static byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    }

    @Test
    @DisplayName("reconhece PDF, JPEG e PNG pelos primeiros bytes")
    void reconheceFormatos() {
        assertThat(FormatoArquivo.detectar(pdf())).isEqualTo(FormatoArquivo.PDF);
        assertThat(FormatoArquivo.detectar(jpeg())).isEqualTo(FormatoArquivo.JPEG);
        assertThat(FormatoArquivo.detectar(png())).isEqualTo(FormatoArquivo.PNG);
    }

    @Test
    @DisplayName("cada formato informa tipo de conteúdo e extensão")
    void tipoEExtensao() {
        assertThat(FormatoArquivo.PDF.tipoConteudo()).isEqualTo("application/pdf");
        assertThat(FormatoArquivo.PDF.extensao()).isEqualTo(".pdf");
        assertThat(FormatoArquivo.JPEG.tipoConteudo()).isEqualTo("image/jpeg");
        assertThat(FormatoArquivo.JPEG.extensao()).isEqualTo(".jpg");
        assertThat(FormatoArquivo.PNG.tipoConteudo()).isEqualTo("image/png");
        assertThat(FormatoArquivo.PNG.extensao()).isEqualTo(".png");
    }

    @Test
    @DisplayName("rejeita executável, texto, HTML e SVG mesmo com nome de imagem")
    void rejeitaOutrosFormatos() {
        byte[][] invalidos = {
                {'M', 'Z', (byte) 0x90, 0x00, 0x03},
                "texto simples".getBytes(),
                "<html><script>alert(1)</script>".getBytes(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(),
                "GIF89a".getBytes()
        };
        for (byte[] b : invalidos) {
            assertThatThrownBy(() -> FormatoArquivo.detectar(b)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("rejeita arquivos curtos demais para a assinatura")
    void rejeitaCurtos() {
        assertThatThrownBy(() -> FormatoArquivo.detectar(new byte[0])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FormatoArquivo.detectar("%PDF".getBytes())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FormatoArquivo.detectar(new byte[]{(byte) 0xFF, (byte) 0xD8}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FormatoArquivo.detectar(new byte[]{(byte) 0x89, 'P', 'N', 'G'}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("assinatura PNG incompleta ou adulterada é rejeitada")
    void rejeitaPngAdulterado() {
        byte[] adulterado = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x00, 0, 0};
        assertThatThrownBy(() -> FormatoArquivo.detectar(adulterado)).isInstanceOf(IllegalArgumentException.class);
    }
}
