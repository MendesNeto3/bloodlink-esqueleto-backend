package com.bloodlink.auth.shared.LogAdapter;

import com.bloodlink.auth.usuario.application.out.EnvioRedefinicaoSenhaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("!prod")
public class EnvioRedefinicaoSenhaLogAdapter implements EnvioRedefinicaoSenhaPort {

    private static final Logger log = LoggerFactory.getLogger(EnvioRedefinicaoSenhaLogAdapter.class);

    private final String urlFront;

    public EnvioRedefinicaoSenhaLogAdapter(@Value("${bloodlink.front.url:http://localhost:3000}") String urlFront) {
        this.urlFront = urlFront;
    }

    @Override
    public void enviar(String email, String nome, String token, Instant expiraEm) {
        // DESENVOLVIMENTO: o link é uma credencial. Em produção este bean não existe.
        log.warn("[DEV] Link de redefinição de senha (válido até {}): {}/redefinir-senha?token={}",
                expiraEm, urlFront, token);
    }
}
