package com.bloodlink.api.doador.application.port.in;

import com.bloodlink.api.doador.application.port.out.AutocadastroComando;

public interface AutocadastroDoadorUseCase {

    /**
     * Cria o doador e a ficha no schema do hemocentro escolhido e pede ao Auth o link de ativação.
     * A resposta é sempre a mesma para quem já tem cadastro, para não revelar quem está cadastrado.
     */
    void cadastrar(AutocadastroComando comando);
}
