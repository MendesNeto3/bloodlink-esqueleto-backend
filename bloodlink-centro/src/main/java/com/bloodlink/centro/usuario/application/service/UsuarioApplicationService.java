package com.bloodlink.centro.usuario.application.service;

import com.bloodlink.auth.JwtSupport;
import com.bloodlink.centro.application.port.out.CentroRepositoryPort;
import com.bloodlink.centro.domain.Centro;
import com.bloodlink.centro.usuario.application.port.in.AutenticarUsuarioUseCase;
import com.bloodlink.centro.usuario.application.port.in.UsuarioUseCase;
import com.bloodlink.centro.usuario.application.port.out.UsuarioRepositoryPort;
import com.bloodlink.centro.usuario.domain.Usuario;
import com.bloodlink.common.correlation.Papel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioApplicationService implements UsuarioUseCase, AutenticarUsuarioUseCase {
    private static final Logger log = LoggerFactory.getLogger(UsuarioApplicationService.class.getName());

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final CentroRepositoryPort centroRepositoryPort;
    private final JwtSupport support;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioApplicationService(UsuarioRepositoryPort usuarioRepositoryPort,
                                     CentroRepositoryPort centroRepositoryPort,
                                     JwtSupport support) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.centroRepositoryPort = centroRepositoryPort;
        this.support = support;
    }

    @Override
    public Optional<String> autenticar(String email, String senha) {
        Optional<Usuario> usuarioEncontrado;

        try {
            usuarioEncontrado = usuarioRepositoryPort.buscarPorEmail(email);
        } catch (Exception e) {
            log.error("Falha ao buscar usuário durante autenticação", e);
            throw new AutenticacaoIndisponivelException();
        }

        if (usuarioEncontrado.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioEncontrado.get();
        boolean senhaValida;

        try {
            senhaValida = passwordEncoder.matches(senha, usuario.senhaHash());
        } catch (IllegalArgumentException e) {
            log.warn("Hash de senha malformado para o email informado", e);
            senhaValida = false;
        }

        if (!senhaValida) {
            return Optional.empty();
        }

        Centro centro;
        try {
            centro = centroRepositoryPort.buscarPorId(usuario.centroId())
                    .orElseThrow(() -> new IllegalStateException("Centro não encontrado: " + usuario.centroId()));
        } catch (Exception e) {
            log.error(
                    "Falha ao buscar centro do usuário durante autenticação", e);
            throw new AutenticacaoIndisponivelException();
        }

        String schemaSemPrefixo = centro.schema().replace("centro_", "");

        String tokenAcesso = support.gerarToken(
                usuario.email(),
                schemaSemPrefixo,
                usuario.papel().toString()
        );

        return Optional.of(tokenAcesso);
    }

    @Override
    public Usuario cadastrar(String email, String senha, Papel papel, UUID centroId) {
        try {
            String senhaHash = passwordEncoder.encode(senha);
            Usuario novoUsuario = new Usuario(null, email, senhaHash, papel, centroId);
            return usuarioRepositoryPort.salvar(novoUsuario);
        } catch (DataIntegrityViolationException e) {
            throw new EmailJaCadastradoException(email);
        } catch (Exception e) {
            log.error("Falha inesperada ao cadastrar usuário", e);
            throw new CadastroIndisponivelException();
        }
    }

    @Override
    public Usuario atualizarPapel(UUID usuarioId, Papel novoPapel) {
        Usuario atualizado = usuarioRepositoryPort.buscarPorId(usuarioId)
                .map(usuario -> new Usuario(
                        usuario.id(),
                        usuario.email(),
                        usuario.senhaHash(),
                        novoPapel,
                        usuario.centroId()
                ))
                .map(usuarioRepositoryPort::salvar)
                .orElseThrow(UsuarioNaoEncontradoException::new);

        log.info("Papel atualizado: usuarioId={}, novoPapel={}", atualizado.id(), atualizado.papel());

        return atualizado;
    }

    @Override
    public List<Usuario> listarPorCentro(UUID centroId) {
        return usuarioRepositoryPort
                .usariosPorCentro(centroId)
                .stream()
                .toList();
    }
}
