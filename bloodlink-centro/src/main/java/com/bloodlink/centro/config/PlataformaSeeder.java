package com.bloodlink.centro.config;

import com.bloodlink.centro.usuario.application.port.out.UsuarioRepositoryPort;
import com.bloodlink.centro.usuario.domain.Usuario;
import com.bloodlink.common.correlation.Papel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PlataformaSeeder implements ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(PlataformaSeeder.class);

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new  BCryptPasswordEncoder();

    private final String emailPlataforma;
    private final String senhaPlataforma;

    public PlataformaSeeder(
            UsuarioRepositoryPort usuarioRepositoryPort,
            @Value("${BLOODLINK_PLATAFORMA_EMAIL:admin@bloodlink.local}") String emailPlataforma,
            @Value("${BLOODLINK_PLATAFORMA_SENHA:}") String senhaPlataforma
    ) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.emailPlataforma = emailPlataforma;
        this.senhaPlataforma = senhaPlataforma;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (usuarioRepositoryPort.existeUsuarioComPapel(Papel.PLATAFORMA)) {
            log.info("Usuario PLATAFORMA ja existe, seed ignorado.");
            return;
        }

        String senhaEfetiva = senhaPlataforma;
        if (senhaEfetiva == null || senhaEfetiva.isBlank()) {
            senhaEfetiva = UUID.randomUUID().toString();
            log.warn("BLOODLINK_PLATAFORMA_SENHA nao configurada. Senha temporaria gerada para '{}': {}",
                    emailPlataforma, senhaEfetiva);
            log.warn("Troque essa senha assim que possivel - ela so aparece uma vez neste log.");
        }

        Usuario plataforma = new Usuario(
                null,
                emailPlataforma,
                bCryptPasswordEncoder.encode(senhaEfetiva),
                Papel.PLATAFORMA,
                null
        );

        usuarioRepositoryPort.salvar(plataforma);
        log.info("Usuario PLATAFORMA criado: {}", emailPlataforma);
    }
}
