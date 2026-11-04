package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.ContaDoadorPort;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.in.EnviarConviteUseCase;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class EnviarConviteService implements EnviarConviteUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnviarConviteService.class);

    private final DoadorRepositoryPort doadores;
    private final ContaDoadorPort contas;

    public EnviarConviteService(DoadorRepositoryPort doadores, ContaDoadorPort contas) {
        this.doadores = doadores;
        this.contas = contas;
    }

    @Override
    public void enviar(UUID doadorId, String centroId) {
        if (doadorId == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        if (centroId == null || centroId.isBlank()) {
            throw new IllegalArgumentException("Não foi possível identificar o hemocentro");
        }
        Doador doador = buscar(doadorId);
        if (!doador.ativo()) {
            throw new RegraDeNegocioException("Doador inativo: reative o cadastro antes de enviar o convite");
        }
        if (doador.email() == null || doador.email().isBlank()) {
            throw new RegraDeNegocioException("O doador não tem e-mail cadastrado");
        }

        contas.solicitarConta(new ContaDoadorPort.SolicitacaoContaDoador(
                doador.nome(), doador.email(), centroId, doador.id()));
        log.info("Convite de acesso enviado ao doador {}", doadorId);
    }

    private Doador buscar(UUID doadorId) {
        Optional<Doador> encontrado;
        try {
            encontrado = doadores.buscarPorId(doadorId);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o doador {} para o convite - causa: {}", doadorId, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("enviar o convite");
        }
        return encontrado.orElseThrow(() -> new RecursoNaoEncontradoException("Doador", doadorId));
    }
}
