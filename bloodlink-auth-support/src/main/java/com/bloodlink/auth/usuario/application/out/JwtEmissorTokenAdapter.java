package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.common.seguranca.JwtSupport;
import com.bloodlink.auth.usuario.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JwtEmissorTokenAdapter implements EmissorTokenPort{

    private final JwtSupport jwtSupport;
    private final Duration validadeAccess;

    JwtEmissorTokenAdapter(JwtSupport jwtSupport,
                           @Value("${bloodlink.jwt.validade-access:15m}") Duration validadeAccess) {
        this.jwtSupport = jwtSupport;
        this.validadeAccess = validadeAccess;
    }

    @Override
    public String emitirAccessToken(Usuario usuario) {
        return jwtSupport.gerarToken(
                usuario.id().toString(),
                usuario.centroId(),
                usuario.papel().name(),
                usuario.doadorId() != null ? usuario.doadorId().toString() : null);
    }

    @Override
    public long validadeAccessSegundos() {
        return validadeAccess.toSeconds();
    }
}

