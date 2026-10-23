package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.ConversorEnum.ConversorEnum;
import com.bloodlink.auth.shared.exceptions.AcessoNegadoException;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.CadastrarUsuarioCommand;
import com.bloodlink.auth.usuario.application.in.CadastrarUsuarioUseCase;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class CadastrarUsuarioService implements CadastrarUsuarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(CadastrarUsuarioService.class);

    private final UsuarioRepositoryPort repositoryPort;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public CadastrarUsuarioService(UsuarioRepositoryPort repositoryPort, PasswordEncoder encoder, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    public Usuario cadastrar(CadastrarUsuarioCommand comando, Solicitante solicitante) {
        if (comando == null || solicitante == null) {
            throw new IllegalArgumentException("Os dados do usuário são obrigatórios");
        }
        if (!solicitante.papel().gerenciaUsuarios()) {
            throw new AcessoNegadoException("O seu papel não permite cadastrar usuários");
        }

        String nome = ValidarUsuario.validarNome(comando.nome());
        String email = ValidarUsuario.validarEmail(comando.email());
        ValidarUsuario.validarSenha(comando.senha());
        Papel papel = ConversorEnum.obrigatorio(Papel.class, "papel", comando.papel());
        validarPapelPermitido(papel, solicitante);
        String centroId = definirCentro(comando.centroId(), papel, solicitante);

        UUID id = UUID.randomUUID();
        Instant agora = Instant.now(clock);
        String hash = encoder.encode(comando.senha());
        Usuario novo = new Usuario(id, email, nome, hash, papel, centroId, true, agora, agora);

        try {
            garantirEmailDisponivel(email);
            Usuario salvo = repositoryPort.salvar(novo);
            log.info("Usuário {} cadastrado (papel={}, centro={})", salvo.id(), papel, centroId);
            return salvo;
        } catch (ConflitoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao cadastrar o usuário {}: {}", id, MascaraLog.causa(e));
            log.debug("Detalhe da falha ao cadastrar o usuário {}", id, e);
            throw new OperacaoUsuarioException("Falha ao cadastrar o usuário", e);
        }
    }

    private void validarPapelPermitido(Papel papel, Solicitante solicitante) {
        if (papel == Papel.DOADOR) {
            throw new IllegalArgumentException(
                    "A conta de doador é criada pelo cadastro do doador, com ativação por e-mail");
        }
        if (papel == Papel.ADMIN_PLATAFORMA) {
            throw new AcessoNegadoException("Administradores da plataforma não são cadastrados por esta rota");
        }
        if (papel == Papel.ADMIN_CENTRO && !solicitante.ehPlataforma()) {
            throw new AcessoNegadoException("Somente a plataforma cadastra administradores de centro");
        }
    }

    /** Devolve o centro final do novo usuário, respeitando o escopo de quem cadastra. */
    private String definirCentro(String centroInformado, Papel papel, Solicitante solicitante) {
        String centroSolicitado = ValidarUsuario.validarCentroId(centroInformado);
        String centro;

        if (solicitante.ehPlataforma()) {
            centro = centroSolicitado;
        } else {
            if (solicitante.centroId() == null) {
                throw new AcessoNegadoException("O seu usuário não está vinculado a um centro");
            }
            if (centroSolicitado != null && !centroSolicitado.equals(solicitante.centroId())) {
                throw new AcessoNegadoException("Você só pode cadastrar usuários do seu próprio centro");
            }
            centro = solicitante.centroId();
        }

        if (papel.exigeCentro() && centro == null) {
            throw new IllegalArgumentException("O centroId é obrigatório para o papel " + papel);
        }
        return centro;
    }

    private void garantirEmailDisponivel(String email) {
        if (repositoryPort.buscarPorEmail(email).isPresent()) {
            throw new ConflitoException("Já existe um usuário com este e-mail");
        }
    }
}