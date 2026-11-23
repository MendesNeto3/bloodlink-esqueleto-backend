package com.bloodlink.api.documento.application.service;

import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.documento.application.port.in.DocumentoUseCase;
import com.bloodlink.api.documento.application.port.out.AnalisadorArquivoPort;
import com.bloodlink.api.documento.application.port.out.ArmazenamentoArquivoPort;
import com.bloodlink.api.documento.application.port.out.DocumentoRepositoryPort;
import com.bloodlink.api.documento.application.port.out.EnviarDocumentoComando;
import com.bloodlink.api.documento.application.port.out.FiltroDocumento;
import com.bloodlink.api.documento.application.port.out.ResultadoAnalise;
import com.bloodlink.api.documento.domain.ArquivoBaixado;
import com.bloodlink.api.documento.domain.CategoriaDocumento;
import com.bloodlink.api.documento.domain.Documento;
import com.bloodlink.api.documento.domain.DocumentoComDoador;
import com.bloodlink.api.documento.exception.OperacaoDocumentoException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import com.bloodlink.common.log.MascaraLog;
import com.bloodlink.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentoService implements DocumentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    static final int TAMANHO_MAXIMO_BYTES = 10 * 1024 * 1024;
    static final int LIMITE_POR_DOADOR = 20;
    private static final int LIMITE_MAXIMO_LISTA = 200;

    private final DocumentoRepositoryPort documentos;
    private final ArmazenamentoArquivoPort armazenamento;
    private final DoadorRepositoryPort doadores;
    private final AnalisadorArquivoPort analisador;
    private final Clock clock;

    public DocumentoService(DocumentoRepositoryPort documentos, ArmazenamentoArquivoPort armazenamento,
                            DoadorRepositoryPort doadores, AnalisadorArquivoPort analisador, Clock clock) {
        this.documentos = documentos;
        this.armazenamento = armazenamento;
        this.doadores = doadores;
        this.analisador = analisador;
        this.clock = clock;
    }

    @Override
    public Documento enviar(EnviarDocumentoComando comando) {
        // ---- tudo é validado antes de gravar qualquer coisa ----
        if (comando == null) {
            throw new IllegalArgumentException("Os dados do documento são obrigatórios");
        }
        byte[] conteudo = comando.conteudo();
        if (conteudo == null || conteudo.length == 0) {
            throw new IllegalArgumentException("Selecione um arquivo para enviar");
        }
        if (conteudo.length > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("O arquivo deve ter no máximo 10 MB");
        }
        FormatoArquivo formato = FormatoArquivo.detectar(conteudo);
        CategoriaDocumento categoria = CategoriaDocumento.de(comando.categoria());
        if (comando.enviadoPeloDoador() && !categoria.doadorPodeEnviar()) {
            throw new IllegalArgumentException("Você pode enviar apenas documentos do tipo EXAME ou TERMO");
        }
        if (comando.enviadoPeloDoador() && comando.doadorId() == null) {
            throw new IllegalArgumentException("Não foi possível identificar o doador");
        }
        if (comando.enviadoPor() == null) {
            throw new IllegalArgumentException("Não foi possível identificar quem está enviando");
        }
        String nome = NomeArquivo.seguro(comando.nomeOriginal(), formato.extensao());

        UUID id = UUID.randomUUID();
        String pasta = comando.doadorId() == null ? "geral" : comando.doadorId().toString();
        String chave = TenantContext.get() + "/" + pasta + "/" + id + formato.extensao();
        Documento novo = new Documento(id, comando.doadorId(), categoria, nome, formato.tipoConteudo(),
                conteudo.length, chave, comando.enviadoPor(), comando.papel(), Instant.now(clock));

        verificarDoador(comando.doadorId());
        verificarCota(comando);
        verificarVirus(id, conteudo);

        gravarArquivo(novo, conteudo);
        try {
            Documento salvo = documentos.salvar(novo);
            log.info("Documento {} enviado ({} bytes, {})", salvo.id(), salvo.tamanhoBytes(), salvo.categoria());
            return salvo;
        } catch (RuntimeException e) {
            log.error("Falha ao registrar o documento {} - causa: {}", id, MascaraLog.causa(e));
            log.debug("Detalhe da falha ao registrar o documento", e);
            removerSemFalhar(chave);   // não deixa arquivo órfão no armazenamento
            throw new OperacaoDocumentoException("enviar o documento", e);
        }
    }

    @Override
    public List<DocumentoComDoador> listar(FiltroDocumento filtro, int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO_LISTA) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e " + LIMITE_MAXIMO_LISTA);
        }
        FiltroDocumento f = filtro == null ? new FiltroDocumento(null, null, null) : filtro;
        try {
            return comNomes(documentos.listar(f, limite));
        } catch (RuntimeException e) {
            log.error("Falha ao listar os documentos - causa: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha ao listar os documentos", e);
            throw new OperacaoDocumentoException("listar os documentos", e);
        }
    }

    @Override
    public List<DocumentoComDoador> listarDoDoador(UUID doadorId) {
        if (doadorId == null) {
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }
        try {
            // o nome do próprio doador não precisa voltar: lista sem consulta extra
            return documentos.listar(new FiltroDocumento(null, doadorId, null), LIMITE_POR_DOADOR).stream()
                    .map(d -> new DocumentoComDoador(d, null))
                    .toList();
        } catch (RuntimeException e) {
            log.error("Falha ao listar os documentos do doador {} - causa: {}", doadorId, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDocumentoException("listar os documentos", e);
        }
    }

    @Override
    public ArquivoBaixado baixar(UUID id, UUID restritoAoDoador) {
        if (id == null) {
            throw new IllegalArgumentException("O id do documento é obrigatório");
        }
        Documento documento;
        try {
            documento = documentos.buscarPorId(id).orElse(null);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o documento {} - causa: {}", id, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDocumentoException("baixar o documento", e);
        }
        if (documento == null
                || (restritoAoDoador != null && !restritoAoDoador.equals(documento.doadorId()))) {
            throw new RecursoNaoEncontradoException("Documento", id);
        }
        byte[] bytes;
        try {
            bytes = armazenamento.ler(documento.chaveArmazenamento());
        } catch (RuntimeException e) {
            log.error("Arquivo do documento {} indisponível - causa: {}", id, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDocumentoException("baixar o documento", e);
        }
        return new ArquivoBaixado(documento.nomeArquivo(), documento.tipoConteudo(), bytes);
    }

    private void verificarDoador(UUID doadorId) {
        if (doadorId == null) {
            return;
        }
        try {
            if (doadores.buscarPorId(doadorId).isEmpty()) {
                throw new RecursoNaoEncontradoException("Doador", doadorId);
            }
        } catch (RecursoNaoEncontradoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o doador {} no envio de documento - causa: {}",
                    doadorId, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDocumentoException("enviar o documento", e);
        }
    }

    /** Limite só para o envio feito pelo próprio doador (rota aberta a qualquer conta ativa). */
    private void verificarCota(EnviarDocumentoComando comando) {
        if (!comando.enviadoPeloDoador()) {
            return;
        }
        try {
            if (documentos.contarPorDoador(comando.doadorId()) >= LIMITE_POR_DOADOR) {
                throw new RegraDeNegocioException(
                        "Você atingiu o limite de " + LIMITE_POR_DOADOR + " documentos enviados");
            }
        } catch (RegraDeNegocioException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao contar os documentos do doador {} - causa: {}",
                    comando.doadorId(), MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDocumentoException("enviar o documento", e);
        }
    }

    /** Roda por último entre as verificações, por ser a mais cara: só arquivos que passaram nas outras chegam aqui. */
    private void verificarVirus(UUID id, byte[] conteudo) {
        ResultadoAnalise resultado = analisador.analisar(conteudo);
        if (resultado == ResultadoAnalise.INFECTADO) {
            log.warn("Documento {} recusado pela verificação de vírus", id);
            throw new IllegalArgumentException("O arquivo foi recusado pela verificação de segurança");
        }
        if (resultado == ResultadoAnalise.INDISPONIVEL) {
            // com a verificação ligada, não guardar o que não pôde ser verificado
            log.error("Documento {} não enviado: verificação de vírus indisponível", id);
            throw new OperacaoDocumentoException("enviar o documento",
                    new IllegalStateException("verificação de vírus indisponível"));
        }
    }

    private void gravarArquivo(Documento documento, byte[] conteudo) {
        try {
            armazenamento.gravar(documento.chaveArmazenamento(), conteudo, documento.tipoConteudo());
        } catch (RuntimeException e) {
            log.error("Falha ao gravar o arquivo do documento {} - causa: {}", documento.id(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao gravar o arquivo", e);
            throw new OperacaoDocumentoException("enviar o documento", e);
        }
    }

    private void removerSemFalhar(String chave) {
        try {
            armazenamento.remover(chave);
        } catch (RuntimeException e) {
            log.warn("Não foi possível remover o arquivo órfão {} - causa: {}", chave, MascaraLog.causa(e));
        }
    }

    private List<DocumentoComDoador> comNomes(List<Documento> lista) {
        Set<UUID> ids = lista.stream().map(Documento::doadorId).filter(i -> i != null)
                .collect(Collectors.toCollection(HashSet::new));
        Map<UUID, Doador> porId = ids.isEmpty() ? new HashMap<>() : doadores.buscarPorIds(ids);
        return lista.stream()
                .map(d -> new DocumentoComDoador(d, d.doadorId() == null || !porId.containsKey(d.doadorId())
                        ? null : porId.get(d.doadorId()).nome()))
                .toList();
    }
}
