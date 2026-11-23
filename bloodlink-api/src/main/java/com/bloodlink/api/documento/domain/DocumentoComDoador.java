package com.bloodlink.api.documento.domain;

public record DocumentoComDoador(Documento documento, String doadorNome) {

    @Override
    public String toString() {
        return "DocumentoComDoador[id=" + documento.id() + "]";
    }
}
