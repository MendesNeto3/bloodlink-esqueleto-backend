package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Cria o primeiro ADMIN_PLATAFORMA quando ainda não existe nenhum e as variáveis
 * BLOODLINK_ADMIN_EMAIL / BLOODLINK_ADMIN_SENHA estão definidas. Sem isso ninguém
 * conseguiria cadastrar o primeiro usuário. Nunca registra e-mail nem senha no log.
 */

@Component
public class AdminInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);

    private final UsuarioRepositoryPort repositoryPort;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String email;
    private final String senha;
    private final String nome;

    public AdminInicialRunner(UsuarioRepositoryPort repositoryPort, PasswordEncoder encoder, Clock clock,
                              @Value("${bloodlink.auth.admin-inicial.email:}") String email,
                              @Value("${bloodlink.auth.admin-inicial.senha:}") String senha,
                              @Value("${bloodlink.auth.admin-inicial.nome:Administrador da Plataforma}") String nome) {
        this.repositoryPort = repositoryPort;
        this.encoder = encoder;
        this.clock = clock;
        this.email = email;
        this.senha = senha;
        this.nome = nome;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (repositoryPort.existePorPapel(Papel.ADMIN_PLATAFORMA)) {
            return;
        }
        if(email.isEmpty() || senha.isEmpty()) {
            log.warn("Não existe ADMIN_PLATAFORMA e BLOODLINK_ADMIN_EMAIL/BLOODLINK_ADMIN_SENHA não foram "
                    + "definidas: ninguém conseguirá cadastrar usuários");
            return;
        }
        String emailValidado = ValidarUsuario.validarEmail(email);
        ValidarUsuario.validarSenha(senha);
        String nomeValido = ValidarUsuario.validarNome(nome);

        Instant agora = Instant.now(clock);
        Usuario admin = new Usuario(UUID.randomUUID(), emailValidado, nomeValido, encoder.encode(senha),
                Papel.ADMIN_PLATAFORMA, null, true, agora, agora);
        try {
            repositoryPort.salvar(admin);
            log.info("Administrador inicial da plataforma criado (id={})", admin.id());
        } catch (ConflitoException e) {
            log.info("Administrador inicial já criado por outra instância");
        }

    }
}
