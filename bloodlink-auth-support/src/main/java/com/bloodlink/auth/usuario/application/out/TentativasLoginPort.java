package com.bloodlink.auth.usuario.application.out;

/** Controle de tentativas de login erradas por e-mail. Falha do armazenamento nunca impede o login. */
public interface TentativasLoginPort {

    /** Segundos até o e-mail poder tentar de novo; 0 se não está bloqueado. */
    long segundosDeBloqueio(String email);

    /** Conta uma tentativa errada. */
    void registrarFalha(String email);

    /** Zera a contagem (login bem-sucedido). */
    void limpar(String email);
}
