package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.CriarContaDoadorUseCase;
import com.bloodlink.auth.usuario.application.in.web.CriarContaDoadorCommand;
import com.bloodlink.auth.usuario.application.out.EnvioAtivacaoPort;
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
import java.util.UUID;

@Service
@Transactional
public class CriarContaDoadorService implements CriarContaDoadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(CriarContaDoadorService.class);

    private final UsuarioRepositoryPort usuarios;
    private final TokenAtivacaoRepositoryPort tokens;
    private final EnvioAtivacaoPort envio;
    private final Clock clock;
    private final Duration validade;

    public CriarContaDoadorService(UsuarioRepositoryPort usuarios, TokenAtivacaoRepositoryPort tokens,
                                   EnvioAtivacaoPort envio, Clock clock,
                                   @Value("${bloodlink.auth.ativacao.validade:48h}") Duration validade) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.envio = envio;
        this.clock = clock;
        this.validade = validade;
    }


    @Override
    public UUID criar(CriarContaDoadorCommand comando) {
        if (comando == null) {
            throw new IllegalArgumentException("Os dados da conta são obrigatórios");
        }
        String nome = ValidarUsuario.validarNome(comando.nome());
        String email = ValidarUsuario.validarEmail(comando.email());
        String centroId = ValidarUsuario.validarCentroId(comando.centroId());
        if (centroId == null || centroId.length() != 12) {
            throw new IllegalArgumentException("centroId inválido: informe o identificador de 12 caracteres");
        }
        if (comando.doadorId() == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }

        Instant agora = Instant.now(clock);
        Instant expiraEm = agora.plus(validade);
        try {
            // e-mail já existente: só reaproveita a MESMA conta ainda pendente (reenvio do link)
            Usuario conta = usuarios.buscarPorEmail(email)
                    .map(existente -> reaproveitar(existente, comando.doadorId(), centroId))
                    .orElseGet(() -> usuarios.salvar(Usuario.doadorPendente(
                            UUID.randomUUID(), email, nome, centroId, comando.doadorId(), agora)));

            tokens.invalidarPendentesDoUsuario(conta.id(), agora);
            String token = TokensOpacos.gerar();
            tokens.salvar(new TokenAtivacao(UUID.randomUUID(), conta.id(),
                    TokensOpacos.hash(token), expiraEm, null, agora));
            envio.enviar(email, nome, token, expiraEm);

            log.info("Conta do doador {} aguardando ativação (usuário {})", comando.doadorId(), conta.id());
            return conta.id();
        } catch (ConflitoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao criar a conta do doador {}: {}", comando.doadorId(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao criar a conta do doador", e);
            throw new OperacaoUsuarioException("Falha ao criar a conta do doador", e);
        }
    }

    private Usuario reaproveitar(Usuario existente, UUID doadorId, String centroId) {
        boolean mesmaContaPendente = existente.pendente()
                && existente.papel() == Papel.DOADOR
                && doadorId.equals(existente.doadorId())
                && centroId.equals(existente.centroId());
        if (!mesmaContaPendente) {
            throw new ConflitoException("Já existe uma conta com este e-mail");
        }
        return existente;
    }
}