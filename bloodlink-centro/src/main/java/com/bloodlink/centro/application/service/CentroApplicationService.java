package com.bloodlink.centro.application.service;

import com.bloodlink.centro.application.port.in.CentroUseCase;
import com.bloodlink.centro.application.port.out.CentroRepositoryPort;
import com.bloodlink.centro.application.port.out.SchemaProvisionerGateway;
import com.bloodlink.centro.domain.Centro;

import com.bloodlink.centro.domain.CentroComDistancia;
import com.bloodlink.centro.domain.PerfilCentro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CentroApplicationService implements CentroUseCase {

    private static final Logger log = LoggerFactory.getLogger(CentroApplicationService.class);

    private static final int NOME_TAMANHO_MAXIMO = 150;
    private static final Pattern FORMATO_CENTRO_ID = Pattern.compile("^[a-f0-9]{12}$");

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

        schemaProvisionerGateway.provisionarEmtodosOsServicos(centroSalvo.id(), schema, centroSalvo.ativo());

        return centroSalvo;
    }

    @Override
    public List<Centro> listar() {
        return centroRepositoryPort.listarTodos();
    }

    @Override
    public List<Centro> listarAtivos() {
        return centroRepositoryPort.listarTodos().stream()
                .filter(Centro::ativo)
                .toList();
    }

    @Override
    public List<CentroComDistancia> listarPublicos(Double lat, Double lng) {
        if ((lat == null) != (lng == null)) {
            throw new IllegalArgumentException("Informe lat e lng juntos");
        }
        if (lat != null && (lat < -90 || lat > 90 || lng < -180 || lng > 180)) {
            throw new IllegalArgumentException("Coordenadas fora do intervalo válido");
        }

        Comparator<CentroComDistancia> porNome =
                Comparator.comparing(c -> c.centro().nome(), String.CASE_INSENSITIVE_ORDER);
        Comparator<CentroComDistancia> ordem = lat == null
                ? porNome
                : Comparator.comparing(CentroComDistancia::distanciaKm,
                Comparator.nullsLast(Comparator.<Double>naturalOrder())).thenComparing(porNome);

        return listarAtivos().stream()
                .map(centro -> new CentroComDistancia(centro, distancia(centro, lat, lng)))
                .sorted(ordem)
                .toList();
    }

    @Override
    public Centro atualizarNome(UUID id, String nome, String centroIdSolicitante) {
        String nomeValidado = validarNome(nome);

        Centro centroAtual = buscarDoSolicitante(id, centroIdSolicitante);
        Centro centroAtualizado = centroAtual.comNome(nomeValidado);

        log.info("Nome do centro atualizado: id={}, nomeAntigo={}, nomeNovo={}",
                centroAtual.id(), centroAtual.nome(), centroAtualizado.nome());

        return centroRepositoryPort.salvar(centroAtualizado);
    }

    @Override
    public Centro atualizarPerfil(UUID id, PerfilCentro perfil, String centroIdSolicitante) {
        if (perfil == null) {
            throw new IllegalArgumentException("O perfil do centro é obrigatório");
        }
        Centro centroAtual = buscarDoSolicitante(id, centroIdSolicitante);

        log.info("Perfil do centro atualizado: id={}", centroAtual.id());

        return centroRepositoryPort.salvar(centroAtual.comPerfil(perfil));
    }

    @Override
    public Centro desativar(UUID id) {
        Centro centroAtual = centroRepositoryPort.buscarPorId(id)
                .orElseThrow(CentroNaoEncontradoException::new);

        Centro centroDesativado = centroRepositoryPort.salvar(centroAtual.desativado());

        log.info("Centro desativado: id={}, nome={}", centroDesativado.id(), centroDesativado.nome());

        return centroDesativado;
    }

    @Override
    public Centro buscarPorCentroId(String centroId) {
        if (centroId == null || !FORMATO_CENTRO_ID.matcher(centroId.trim()).matches()) {
            throw new IllegalArgumentException("centroId inválido");
        }
        return centroRepositoryPort.buscarPorSchema("centro_" + centroId.trim())
                .orElseThrow(CentroNaoEncontradoException::new);
    }

    /** Quem não é da plataforma só enxerga o próprio centro; os outros respondem como "não encontrado". */
    private Centro buscarDoSolicitante(UUID id, String centroIdSolicitante) {
        Centro centro = centroRepositoryPort.buscarPorId(id)
                .orElseThrow(CentroNaoEncontradoException::new);
        if (centroIdSolicitante != null && !centro.centroId().equals(centroIdSolicitante.trim())) {
            log.warn("Tentativa de alterar um centro que não é do solicitante: centro={}", id);
            throw new CentroNaoEncontradoException();
        }
        return centro;
    }

    private static Double distancia(Centro centro, Double lat, Double lng) {
        PerfilCentro perfil = centro.perfil();
        if (lat == null || perfil.latitude() == null) {
            return null;
        }
        return DistanciaGeografica.km(lat, lng, perfil.latitude().doubleValue(), perfil.longitude().doubleValue());
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