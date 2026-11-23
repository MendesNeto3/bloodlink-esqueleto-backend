package com.bloodlink.api.documento.application.service;

/** Formatos aceitos, reconhecidos pelos primeiros bytes (o que o navegador diz sobre o arquivo não vale). */
enum FormatoArquivo {
    PDF("application/pdf", ".pdf"),
    JPEG("image/jpeg", ".jpg"),
    PNG("image/png", ".png");

    private final String tipoConteudo;
    private final String extensao;

    FormatoArquivo(String tipoConteudo, String extensao) {
        this.tipoConteudo = tipoConteudo;
        this.extensao = extensao;
    }

    String tipoConteudo() {
        return tipoConteudo;
    }

    String extensao() {
        return extensao;
    }

    static FormatoArquivo detectar(byte[] b) {
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
            return PDF;
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return PNG;
        }
        throw new IllegalArgumentException("Formato não aceito: envie um arquivo PDF, JPG ou PNG");
    }
}
