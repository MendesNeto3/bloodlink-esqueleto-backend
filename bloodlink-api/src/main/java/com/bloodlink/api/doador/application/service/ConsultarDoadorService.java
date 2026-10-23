package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.ConsultarDoadorUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Transactional(readOnly = true)
public class ConsultarDoadorService implements ConsultarDoadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultarDoadorService.class);
    private static final String NAO_ENCONTRADO = "Doador não encontrado";

    private final DoadorRepositoryPort repository;

    public ConsultarDoadorService(DoadorRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Doador porId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        Optional<Doador> encontrado;
        try {
            encontrado = repository.buscarPorId(id);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o doador {}: {}", id, MascaraLog.causa(e));
            log.debug("Detalhe da falha ao consultar o doador", e);
            throw new OperacaoDoadorException("consultar o doador");
        }
        return encontrado.orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
    }

    @Override
    public Doador porCpf(String cpf) {
        String digitos = ValidacoesDoador.validarCpf(cpf);
        return buscar(() -> repository.buscarPorCpf(digitos), "por CPF");
    }

    @Override
    public Doador porEmail(String email) {
        String emailValidado = ValidacoesDoador.validarEmail(email);
        if (emailValidado == null) {
            throw new IllegalArgumentException("O e-mail é obrigatório");
        }
        return buscar(() -> repository.buscarPorEmail(emailValidado), "por e-mail");
    }

    @Override
    public Doador porNome(String nome) {
        String nomeValidado = ValidacoesDoador.validarNome(nome);
        return buscar(() -> repository.buscarPorNome(nomeValidado), "por nome");
    }

    /** CPF, e-mail e nome são dados pessoais: nunca vão ao log, só o tipo da busca. */
    private Doador buscar(Supplier<Optional<Doador>> consulta, String criterio) {
        Optional<Doador> encontrado;
        try {
            encontrado = consulta.get();
        } catch (ConflitoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o doador {} - causa: {}", criterio, MascaraLog.causa(e));
            log.debug("Detalhe da falha ao consultar o doador", e);
            throw new OperacaoDoadorException("consultar");
        }
        return encontrado.orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
    }
}