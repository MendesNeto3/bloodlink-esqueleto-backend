package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.AtivarContaUseCase;
import com.bloodlink.auth.usuario.application.in.web.AtivarContaCommand;
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
public class AtivarContaService implements AtivarContaUseCase {

    private static final Logger log = LoggerFactory.getLogger(AtivarContaService.class);
    private static final String LINK_INVALIDO = "Link de ativação inválido ou expirado";

    private final UsuarioRepositoryPort usuarios;
    private final TokenAtivacaoRepositoryPort tokens;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public AtivarContaService(UsuarioRepositoryPort usuarios, TokenAtivacaoRepositoryPort tokens,
                              PasswordEncoder encoder, Clock clock) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    public void ativar(AtivarContaCommand comando) {
        if (comando == null || comando.token() == null || comando.token().isBlank()) {
            throw new IllegalArgumentException("O token de ativação é obrigatório");
        }
        ValidarUsuario.validarSenha(comando.senha());
        Instant agora = Instant.now(clock);

        try {
            TokenAtivacao token = tokens.buscarPorHash(TokensOpacos.hash(comando.token().trim()))
                    .filter(t -> t.usadoEm() == null && !t.expirado(agora))
                    .orElseThrow(() -> new IllegalArgumentException(LINK_INVALIDO));
            Usuario usuario = usuarios.buscarPorId(token.usuarioId())
                    .filter(u -> u.pendente() && u.ativo())
                    .orElseThrow(() -> new IllegalArgumentException(LINK_INVALIDO));

            // consome o token de forma atômica: dois cliques simultâneos não ativam duas vezes
            if (!tokens.marcarUsadoSeDisponivel(token.id(), agora)) {
                throw new IllegalArgumentException(LINK_INVALIDO);
            }
            usuarios.salvar(usuario.ativadoComSenha(encoder.encode(comando.senha()), agora));
            log.info("Conta do usuário {} ativada", usuario.id());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao ativar a conta: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha ao ativar a conta", e);
            throw new OperacaoUsuarioException("Falha ao ativar a conta", e);
        }
    }
}