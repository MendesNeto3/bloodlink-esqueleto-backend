package com.bloodlink.api.documento.application.port.out;

/** Verifica o conteúdo de um arquivo antes de ele ser guardado. Nunca lança: falha vira INDISPONIVEL. */
public interface AnalisadorArquivoPort {

    ResultadoAnalise analisar(byte[] conteudo);
}
