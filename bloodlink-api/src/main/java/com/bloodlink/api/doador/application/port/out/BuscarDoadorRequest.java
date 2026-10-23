package com.bloodlink.api.doador.application.port.out;

public record BuscarDoadorRequest(String cpf, String email, String nome) {

    @Override
    public String toString() {
        return "BuscarDoadorRequest[]";
    }
}
