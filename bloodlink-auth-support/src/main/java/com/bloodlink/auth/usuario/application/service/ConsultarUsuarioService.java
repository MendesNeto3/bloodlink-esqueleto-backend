package com.bloodlink.auth.usuario.application.service;


import com.bloodlink.auth.shared.exceptions.AcessoNegadoException;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.ConsultarUsuarioUseCase;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;

import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConsultarUsuarioService implements ConsultarUsuarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultarUsuarioService.class);
    private static final String NAO_ENCONTRADO = "Usuário não encontrado";

    private final UsuarioRepositoryPort repositoryPort;

    public ConsultarUsuarioService(UsuarioRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Usuario porId(UUID id, Solicitante solicitante) {
        if (id == null) {
            throw new IllegalArgumentException("O id do usuário é obrigatório");
        }
        return repositoryPort.buscarPorId(id)
                .filter(u -> EscopoUsuario.enxerga(solicitante, u))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    @Override
    public Usuario porEmail(String email, Solicitante solicitante) {
        String limpo = ValidarUsuario.validarEmail(email);
        Optional<Usuario> encontrado;
        try {
            encontrado = repositoryPort.buscarPorEmail(limpo);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar usuário por e-mail - causa: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoUsuarioException("Falha ao consultar o usuário", e);
        }
        return encontrado
                .filter(u -> EscopoUsuario.enxerga(solicitante, u))
                .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
    }

    @Override
    public Usuario porNome(String nome, Solicitante solicitante) {
        if (solicitante.papel() == Papel.RECEPCAO) {
            throw new AcessoNegadoException("O seu papel não permite consultar usuários por nome");
        }
        String limpo = ValidarUsuario.validarNome(nome);
        String centro = solicitante.ehPlataforma() ? null : solicitante.centroId();
        List<Usuario> encontrados;
        try {
            encontrados = repositoryPort.buscarPorNome(limpo, centro);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar usuário por nome - causa: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoUsuarioException("Falha ao consultar o usuário", e);
        }
        if (encontrados.size() > 1) {
            throw new ConflitoException("Há mais de um usuário com este nome; consulte pelo e-mail");
        }
        return encontrados.stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
    }
}