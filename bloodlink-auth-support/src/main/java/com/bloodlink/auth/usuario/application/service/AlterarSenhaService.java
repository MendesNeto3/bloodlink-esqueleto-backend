package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.AlterarSenhaCommand;
import com.bloodlink.auth.usuario.application.in.AlterarSenhaUseCase;
import com.bloodlink.auth.usuario.application.out.RevogarSessoesPort;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AlterarSenhaService implements AlterarSenhaUseCase {

    private static final Logger log = LoggerFactory.getLogger(AlterarSenhaService.class);

    private final UsuarioRepositoryPort repositoryPort;
    private final RevogarSessoesPort sessoesPort;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public AlterarSenhaService(UsuarioRepositoryPort repositoryPort,
                               RevogarSessoesPort sessoesPort,
                               PasswordEncoder encoder,
                               Clock clock) {
        this.repositoryPort = repositoryPort;
        this.sessoesPort = sessoesPort;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void alterar(AlterarSenhaCommand comando, Solicitante solicitante) {
        if (comando == null || solicitante == null) {
            throw new IllegalArgumentException("Os dados da troca de senha são obrigatórios");
        }
        if (comando.senhaAtual() == null || comando.senhaAtual().isEmpty()) {
            throw new IllegalArgumentException("A senha atual é obrigatória");
        }
        ValidarUsuario.validarSenha(comando.novaSenha());

        Usuario usuario = repositoryPort.buscarPorId(solicitante.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", solicitante.id()));
        if (!usuario.ativo()) {
            throw new RegraDeNegocioException("Usuário inativo");
        }
        if (!encoder.matches(comando.senhaAtual(), usuario.senhaHash())) {
            throw new RegraDeNegocioException("Senha atual incorreta");
        }
        if (encoder.matches(comando.novaSenha(), usuario.senhaHash())) {
            throw new RegraDeNegocioException("A nova senha deve ser diferente da atual");
        }

        Usuario atualizado = usuario.comSenha(encoder.encode(comando.novaSenha()), Instant.now(clock));
        try {
            repositoryPort.salvar(atualizado);
            sessoesPort.revogarTodasDoUsuario(usuario.id());
            log.info("Senha do usuário {} alterada", usuario.id());
        } catch (RuntimeException e) {
            log.error("Falha ao alterar a senha do usuário {}: {}", usuario.id(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao alterar a senha", e);
            throw new OperacaoUsuarioException("Falha ao alterar a senha", e);
        }
    }
}