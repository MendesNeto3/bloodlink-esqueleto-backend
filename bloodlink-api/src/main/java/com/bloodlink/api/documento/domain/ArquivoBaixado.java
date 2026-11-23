package com.bloodlink.api.documento.domain;

public record ArquivoBaixado(String nome, String tipoConteudo, byte[] bytes) {

    @Override
    public String toString() {
        return "ArquivoBaixado[" + bytes.length + " bytes]";
    }
}
