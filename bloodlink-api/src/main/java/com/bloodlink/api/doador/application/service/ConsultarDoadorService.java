package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.ConsultarDoadorUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.shared.Exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ConsultarDoadorService implements ConsultarDoadorUseCase {

    private final DoadorRepositoryPort repositoryPort;

    public ConsultarDoadorService (DoadorRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Doador porId(UUID id) {
        return repositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doador", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Doador porCpf(String cpf) {
        String digitos = ValidacoesDoador.validarCpf(cpf);
        return repositoryPort.buscarPorCpf(digitos)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doador", "com o CPF informado"));
    }
}
