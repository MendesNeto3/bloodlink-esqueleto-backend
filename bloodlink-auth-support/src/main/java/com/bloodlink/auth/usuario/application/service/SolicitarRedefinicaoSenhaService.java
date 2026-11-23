package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.SolicitarRedefinicaoSenhaUseCase;
import com.bloodlink.auth.usuario.application.out.EnvioRedefinicaoSenhaPort;
import com.bloodlink.auth.usuario.application.out.TokenAtivacaoRepositoryPort;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SolicitarRedefinicaoSenhaService implements SolicitarRedefinicaoSenhaUseCase {

    private static final Logger log = LoggerFactory.getLogger(SolicitarRedefinicaoSenhaService.class);
    private static final Duration JANELA_DO_LIMITE = Duration.ofHours(1);

    private final UsuarioRepositoryPort usuarios;
    private final TokenAtivacaoRepositoryPort tokens;
    private final EnvioRedefinicaoSenhaPort envio;
    private final Clock clock;
    private final Duration validade;
    private final int maxPorHora;

    public SolicitarRedefinicaoSenhaService(UsuarioRepositoryPort usuarios, TokenAtivacaoRepositoryPort tokens,
                                            EnvioRedefinicaoSenhaPort envio, Clock clock,
                                            @Value("${bloodlink.auth.redefinicao.validade:1h}") Duration validade,
                                            @Value("${bloodlink.auth.redefinicao.max-por-hora:3}") int maxPorHora) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.envio = envio;
        this.clock = clock;
        this.validade = validade;
        this.maxPorHora = maxPorHora;
    }

    @Override
    public void solicitar(String email) {
        String limpo = ValidarUsuario.validarEmail(email);
        Instant agora = Instant.now(clock);

        Usuario conta;
        try {
            Optional<Usuario> encontrada = usuarios.buscarPorEmail(limpo).filter(Usuario::podeLogar);
            if (encontrada.isEmpty()) {
                // sem conta apta (inexistente, pendente de ativação ou inativa): mesma resposta, nada é enviado
                log.info("Pedido de redefinição de senha sem conta apta");
                return;
            }
            conta = encontrada.get();
            if (tokens.contarCriadosDesde(conta.id(), agora.minus(JANELA_DO_LIMITE)) >= maxPorHora) {
                log.warn("Pedido de redefinição de senha do usuário {} barrado pelo limite por hora", conta.id());
                return;
            }
            tokens.invalidarPendentesDoUsuario(conta.id(), agora);
        } catch (RuntimeException e) {
            log.error("Falha ao preparar a redefinição de senha: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha ao preparar a redefinição de senha", e);
            throw new OperacaoUsuarioException("Falha ao solicitar a redefinição de senha", e);
        }

        String token = TokensOpacos.gerar();
        Instant expiraEm = agora.plus(validade);
        try {
            tokens.salvar(new TokenAtivacao(UUID.randomUUID(), conta.id(), TokensOpacos.hash(token),
                    expiraEm, null, agora));
        } catch (RuntimeException e) {
            log.error("Falha ao gravar o token de redefinição do usuário {}: {}", conta.id(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao gravar o token", e);
            throw new OperacaoUsuarioException("Falha ao solicitar a redefinição de senha", e);
        }
        try {
            envio.enviar(conta.email(), conta.nome(), token, expiraEm);
            log.info("Link de redefinição de senha enviado ao usuário {}", conta.id());
        } catch (RuntimeException e) {
            // falha de e-mail só acontece para quem tem conta: se subisse, revelaria quem está cadastrado
            log.error("Falha ao enviar o link de redefinição ao usuário {}: {}", conta.id(), MascaraLog.causa(e));
            log.debug("Detalhe da falha no envio", e);
        }
    }
}
