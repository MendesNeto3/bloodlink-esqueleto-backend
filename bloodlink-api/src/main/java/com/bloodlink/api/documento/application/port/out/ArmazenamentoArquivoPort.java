package com.bloodlink.api.documento.application.port.out;

/** Guarda os bytes do arquivo (S3, MinIO...). O banco guarda só os dados do documento e a chave. */
public interface ArmazenamentoArquivoPort {

    void gravar(String chave, byte[] conteudo, String tipoConteudo);

    byte[] ler(String chave);

    void remover(String chave);
}
