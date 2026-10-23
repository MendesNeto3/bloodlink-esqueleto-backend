package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.AtualizarPerfilDoadorUseCase;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.out.AtualizarPerfilDoadorCommand;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AtualizarPerfilDoadorService implements AtualizarPerfilDoadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(AtualizarPerfilDoadorService.class);

    private final DoadorRepositoryPort repositoryPort;
    private final Clock clock;

    public AtualizarPerfilDoadorService(DoadorRepositoryPort repositoryPort, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.clock = clock;
    }

    @Override
    public Doador atualizar(AtualizarPerfilDoadorCommand comando) {
        if (comando == null) {
            throw new IllegalArgumentException("Dados do perfil não informados");
        }
        UUID id = comando.doadorId();
        if (id == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        BigDecimal peso = ValidacoesDoador.validarPeso(comando.pesoKg());
        String telefone = ValidacoesDoador.validarTelefone(comando.telefone());
        String cidade = ValidacoesDoador.validarCidade(comando.cidade());
        String uf = ValidacoesDoador.validarUf(comando.uf());

        if ((cidade == null) != (uf == null)) {
            throw new IllegalArgumentException("Informe a cidade e a UF juntas");
        }

        Doador atual = buscar(id);
        if (Objects.equals(atual.telefone(), telefone)
                && Objects.equals(atual.cidade(), cidade)
                && Objects.equals(atual.uf(), uf)
                && atual.pesoKg().compareTo(peso) == 0) {
            log.info("Perfil do doador {} sem alterações; nada foi gravado", id);
            return atual;
        }

        try {
            Doador salvo = repositoryPort.salvar(atual.comPerfil(telefone, cidade, uf, peso, clock.instant()));
            log.info("Perfil do doador {} atualizado", id);
            return salvo;
        } catch (RuntimeException e) {
            log.error("Falha ao atualizar o perfil do doador {} - causa: {}", id, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("atualizar o perfil");
        }
    }

    private Doador buscar(UUID id) {
        Optional<Doador> encontrado;
        try {
            encontrado = repositoryPort.buscarPorId(id);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o doador {} para atualizar o perfil - causa: {}", id, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("atualizar o perfil");
        }
        return encontrado.orElseThrow(() -> new RecursoNaoEncontradoException("Doador", id));
    }
}
