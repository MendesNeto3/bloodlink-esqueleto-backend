package com.bloodlink.auth.usuario.application.out;

import java.time.Instant;

public interface EnvioAtivacaoPort {
    /** O token em texto só existe aqui e no e-mail; no banco fica apenas o hash. */
    void enviar(String email, String nome, String token, Instant expiraEm);
}