package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.RedefinirSenhaUseCase;
import com.bloodlink.auth.usuario.application.in.web.RedefinirSenhaCommand;
import com.bloodlink.auth.usuario.application.out.RevogarSessoesPort;
import com.bloodlink.auth.usuario.application.out.TentativasLoginPort;
import com.bloodlink.auth.usuario.application.out.TokenAtivacaoRepositoryPort;
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
@Transactional
public class RedefinirSenhaService implements RedefinirSenhaUseCase {

    private static final Logger log = LoggerFactory.getLogger(RedefinirSenhaService.class);
    private static final String LINK_INVALIDO = "Link de redefinição inválido ou expirado";

    private final UsuarioRepositoryPort usuarios;
    private final TokenAtivacaoRepositoryPort tokens;
    private final RevogarSessoesPort sessoes;
    private final TentativasLoginPort tentativas;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public RedefinirSenhaService(UsuarioRepositoryPort usuarios, TokenAtivacaoRepositoryPort tokens,
                                 RevogarSessoesPort sessoes, TentativasLoginPort tentativas,
                                 PasswordEncoder encoder, Clock clock) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.sessoes = sessoes;
        this.tentativas = tentativas;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    public void redefinir(RedefinirSenhaCommand comando) {
        if (comando == null || comando.token() == null || comando.token().isBlank()) {
            throw new IllegalArgumentException("O token de redefinição é obrigatório");
        }
        ValidarUsuario.validarSenha(comando.senha());
        Instant agora = Instant.now(clock);

        try {
            TokenAtivacao token = tokens.buscarPorHash(TokensOpacos.hash(comando.token().trim()))
                    .filter(t -> t.usadoEm() == null && !t.expirado(agora))
                    .orElseThrow(() -> new IllegalArgumentException(LINK_INVALIDO));
            // só conta já ativa e liberada: um link de ativação não serve para redefinir senha
            Usuario usuario = usuarios.buscarPorId(token.usuarioId())
                    .filter(Usuario::podeLogar)
                    .orElseThrow(() -> new IllegalArgumentException(LINK_INVALIDO));

            // consome o token de forma atômica: dois cliques simultâneos não redefinem duas vezes
            if (!tokens.marcarUsadoSeDisponivel(token.id(), agora)) {
                throw new IllegalArgumentException(LINK_INVALIDO);
            }
            usuarios.salvar(usuario.comSenha(encoder.encode(comando.senha()), agora));
            tokens.invalidarPendentesDoUsuario(usuario.id(), agora);
            sessoes.revogarTodasDoUsuario(usuario.id());
            tentativas.limpar(usuario.email());
            log.info("Senha do usuário {} redefinida", usuario.id());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao redefinir a senha: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha ao redefinir a senha", e);
            throw new OperacaoUsuarioException("Falha ao redefinir a senha", e);
        }
    }
}
