package com.bloodlink.api.documento.application.service;

/** Limpa o nome que o usuário deu ao arquivo. O nome só serve para exibir: a chave de armazenamento é gerada aqui. */
final class NomeArquivo {

    private static final int TAMANHO_MAXIMO_SEM_EXTENSAO = 100;

    private NomeArquivo() {
    }

    /** Tira caminho, caracteres de controle e símbolos de arquivo, e troca a extensão pela do formato real. */
    static String seguro(String original, String extensao) {
        String base = original == null ? "" : original;
        int corte = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        base = base.substring(corte + 1);
        base = base.replaceAll("[\\p{Cntrl}\"*:<>?|]", "").replaceAll("\\s+", " ").strip();
        int ponto = base.lastIndexOf('.');
        // ponto no início (ex.: ".pdf") é só extensão, não nome
        String semExtensao = (ponto >= 0 ? base.substring(0, ponto) : base).strip();
        if (semExtensao.isEmpty()) {
            semExtensao = "documento";
        }
        if (semExtensao.length() > TAMANHO_MAXIMO_SEM_EXTENSAO) {
            semExtensao = semExtensao.substring(0, TAMANHO_MAXIMO_SEM_EXTENSAO).strip();
        }
        return semExtensao + extensao;
    }
}
