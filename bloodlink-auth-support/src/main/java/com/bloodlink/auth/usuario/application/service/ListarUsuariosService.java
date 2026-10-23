package com.bloodlink.auth.usuario.application.service;

import com.bloodlink.auth.shared.ConversorEnum.ConversorEnum;
import com.bloodlink.auth.shared.exceptions.AcessoNegadoException;
import com.bloodlink.auth.shared.exceptions.OperacaoUsuarioException;
import com.bloodlink.auth.shared.pagina.Pagina;
import com.bloodlink.auth.shared.pagina.Paginacao;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;
import com.bloodlink.auth.usuario.application.in.ListarUsuariosUseCase;
import com.bloodlink.auth.usuario.application.out.FiltroUsuario;
import com.bloodlink.auth.usuario.application.out.UsuarioRepositoryPort;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ListarUsuariosService implements ListarUsuariosUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListarUsuariosService.class);

    private final UsuarioRepositoryPort repositoryPort;

    public ListarUsuariosService(UsuarioRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Pagina<Usuario> listar(String email, String papel, String centroId, Boolean ativo,
                                  int pagina, int tamanho, Solicitante solicitante) {
        if (!solicitante.papel().gerenciaUsuarios()) {
            throw new AcessoNegadoException("O seu papel não permite listar usuários");
        }
        Paginacao.validar(pagina, tamanho);

        FiltroUsuario filtro = new FiltroUsuario(
                (email == null || email.isBlank()) ? null : email.trim().toLowerCase(Locale.ROOT),
                ConversorEnum.opcional(Papel.class, "papel", papel),
                solicitante.ehPlataforma() ? ValidarUsuario.validarCentroId(centroId) : solicitante.centroId(),
                ativo);

        try {
            return repositoryPort.listar(filtro, pagina, tamanho);
        } catch (RuntimeException e) {
            log.error("Falha ao listar usuários (página={}, tamanho={}): {}", pagina, tamanho, MascaraLog.causa(e));
            log.debug("Detalhe da falha ao listar usuários", e);
            throw new OperacaoUsuarioException("Falha ao listar os usuários", e);
        }
    }
}