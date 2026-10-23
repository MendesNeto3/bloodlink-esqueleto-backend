package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.AcessoNegadoException;
import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.AlterarSituacaoUsuarioUseCase;
import com.bloodlink.auth.usuario.application.out.RevogarSessoesPort;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AlterarSituacaoUsuarioService implements AlterarSituacaoUsuarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(AlterarSituacaoUsuarioService.class);

    private final UsuarioRepositoryPort repositoryPort;
    private final RevogarSessoesPort sessoesPort;
    private final Clock clock;

    public AlterarSituacaoUsuarioService(UsuarioRepositoryPort repositoryPort, RevogarSessoesPort sessoesPort, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.sessoesPort = sessoesPort;
        this.clock = clock;
    }

    @Override
    public Usuario inativar(UUID id, Solicitante solicitante) {
        Usuario usuario = localizar(id, solicitante);
        if (usuario.id().equals(solicitante.id())) {
            throw new RegraDeNegocioException("Você não pode inativar a própria conta");
        }
        if (!usuario.ativo()) {
            return usuario;
        }
        try {
            Usuario salvo = repositoryPort.salvar(usuario.inativado(Instant.now(clock)));
            sessoesPort.revogarTodasDoUsuario(id); // sessão aberta não sobrevive à inativação
            log.info("Usuário {} inativado", id);
            return salvo;
        } catch (RuntimeException e) {
            log.error("Falha ao inativar o usuário {}", id, e);
            throw new OperacaoUsuarioException("Falha ao inativar o usuário", e);
        }
    }

    @Override
    public Usuario reativar(UUID id, Solicitante solicitante) {
        Usuario usuario = localizar(id, solicitante);
        if (usuario.ativo()) {
            return usuario;
        }
        try {
            Usuario salvo = repositoryPort.salvar(usuario.reativado(Instant.now(clock)));
            log.info("Usuário {} reativado", id);
            return salvo;
        } catch (RuntimeException e) {
            log.error("Falha ao reativar o usuário {}", id, e);
            throw new OperacaoUsuarioException("Falha ao reativar o usuário", e);
        }
    }

    private Usuario localizar(UUID id, Solicitante solicitante) {
        if (id == null) {
            throw new IllegalArgumentException("O id do usuário é obrigatório");
        }
        if (solicitante.papel() == Papel.RECEPCAO) {
            throw new AcessoNegadoException("O seu papel não permite alterar a situação de usuários");
        }
        return repositoryPort.buscarPorId(id)
                .filter(u -> EscopoUsuario.enxerga(solicitante, u))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }
}
