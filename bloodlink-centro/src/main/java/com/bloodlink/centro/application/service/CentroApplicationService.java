package com.bloodlink.centro.application.service;

import com.bloodlink.centro.application.port.in.CentroUseCase;
import com.bloodlink.centro.application.port.out.CentroRepositoryPort;
import com.bloodlink.centro.application.port.out.SchemaProvisionerGateway;
import com.bloodlink.centro.domain.Centro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CentroApplicationService implements CentroUseCase {

    private static final Logger log = LoggerFactory.getLogger(CentroApplicationService.class);

    private static final int NOME_TAMANHO_MAXIMO = 150;

    private final CentroRepositoryPort centroRepositoryPort;
    private final SchemaProvisionerGateway schemaProvisionerGateway;

    public CentroApplicationService(CentroRepositoryPort centroRepositoryPort,
                                    SchemaProvisionerGateway schemaProvisionerGateway) {
        this.centroRepositoryPort = centroRepositoryPort;
        this.schemaProvisionerGateway = schemaProvisionerGateway;
    }

    @Override
    public Centro registrar(String nome) {
        String nomeValidado = validarNome(nome);

        String schemaSemPrefixo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String schema = "centro_" + schemaSemPrefixo;

        Centro centroNovo = new Centro(null, nomeValidado, schema, true);
        Centro centroSalvo = centroRepositoryPort.salvar(centroNovo);

        log.info("Centro registrado: id={}, nome={}", centroSalvo.id(), centroSalvo.nome());

        log.debug("Centro {} provisionado no schema {}", centroSalvo.id(), schema);

        schemaProvisionerGateway.provisionarEmtodosOsServicos(schema);

        return centroSalvo;
    }

    @Override
    public List<Centro> listar() {
        return centroRepositoryPort.listarTodos();
    }

    @Override
    public Centro atualizarNome(UUID id, String nome) {
        String nomeValidado = validarNome(nome).trim();

        Centro centroAtual = centroRepositoryPort.buscarPorId(id)
                .orElseThrow(CentroNaoEncontradoException::new);

        Centro centroAtualizado = new Centro(
                centroAtual.id(),
                nomeValidado,
                centroAtual.schema(),
                centroAtual.ativo()
        );

        log.info("Nome do centro atualizado: id={}, nomeAntigo={}, nomeNovo={}",
                centroAtual.id(), centroAtual.nome(), centroAtualizado.nome());

        return centroRepositoryPort.salvar(centroAtualizado);
    }

    @Override
    public Centro desativar(UUID id) {
        Centro centroDesativado = centroRepositoryPort.buscarPorId(id)
                .map(centro -> new Centro
                        (centro.id(),
                         centro.nome(),
                         centro.schema(),
                         false))
                .orElseThrow(CentroNaoEncontradoException::new);

        log.info("Centro desativado: id={}, nome={}", centroDesativado.id(), centroDesativado.nome());

        return centroDesativado;
    }

    private String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do centro não pode ser vazio");
        }
        if (nome.trim().length() > NOME_TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("Nome do centro excede o tamanho máximo de " + NOME_TAMANHO_MAXIMO + " caracteres");
        }
        return nome.trim();
    }
}

