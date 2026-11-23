package com.bloodlink.auth.usuario.application.in;

public interface SolicitarRedefinicaoSenhaUseCase {

    /**
     * Pede o link de redefinição de senha. O resultado é sempre o mesmo, exista ou não uma conta
     * apta para o e-mail: quem pergunta não descobre quem está cadastrado.
     */
    void solicitar(String email);
}
