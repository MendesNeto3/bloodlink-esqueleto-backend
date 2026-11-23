package com.bloodlink.api.agendamento.adapter.out.http;

import com.bloodlink.api.agendamento.application.port.out.CentroConsultaPort;
import com.bloodlink.common.cache.CacheJson;
import com.bloodlink.common.cache.ChavesCache;
import com.bloodlink.common.log.MascaraLog;
import com.bloodlink.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

/**
 * Consulta o nome do hemocentro na rota interna do Centro, com cache no Redis (compartilhado entre as
 * instâncias; o Centro apaga a chave quando o nome muda). Nunca lança: sem resposta, o e-mail sai com
 * o nome padrão, e essa falha também fica em cache por 1 minuto para não repetir a chamada a cada e-mail.
 */
@Component
public class CentroHttpAdapter implements CentroConsultaPort {

    private static final Logger log = LoggerFactory.getLogger(CentroHttpAdapter.class);
    private static final String HEADER_CHAVE = "X-Servico-Chave";
    private static final String PREFIXO_SCHEMA = "centro_";
    static final String NOME_PADRAO = "seu hemocentro";
    private static final Duration VALIDADE_NOME = Duration.ofMinutes(10);
    private static final Duration VALIDADE_FALHA = Duration.ofMinutes(1);
    private static final TypeReference<NomeEmCache> TIPO_NOME = new TypeReference<NomeEmCache>() { };

    /** padrao = true quando o nome é o genérico, guardado só porque a consulta ao Centro falhou. */
    private record NomeEmCache(String nome, boolean padrao) { }

    private record CentroResposta(String centroId, String nome, Boolean ativo) { }

    private final RestClient client;
    private final String chave;
    private final CircuitBreaker circuitBreaker;
    private final CacheJson cache;

    public CentroHttpAdapter(@Value("${bloodlink.centro.url:http://localhost:8084}") String urlCentro,
                             @Value("${bloodlink.servico.chave:}") String chave,
                             CircuitBreakerRegistry registry,
                             CacheJson cache) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().baseUrl(urlCentro).requestFactory(fabrica).build();
        this.chave = chave;
        this.circuitBreaker = registry.circuitBreaker("centro");
        this.cache = cache;
    }

    @Override
    public String nomeDoCentroAtual() {
        String schema = TenantContext.get();
        if (!schema.startsWith(PREFIXO_SCHEMA)) {
            return NOME_PADRAO;
        }
        String centroId = schema.substring(PREFIXO_SCHEMA.length());
        String chaveCache = ChavesCache.nomeCentro(centroId);

        Optional<NomeEmCache> emCache = cache.ler(chaveCache, TIPO_NOME);
        if (emCache.isPresent()) {
            return emCache.get().nome();
        }
        try {
            String nome = circuitBreaker.executeSupplier(() -> buscar(centroId));
            cache.gravar(chaveCache, new NomeEmCache(nome, false), VALIDADE_NOME);
            return nome;
        } catch (RuntimeException e) {
            log.warn("Não foi possível obter o nome do centro do schema {} - causa: {}. Usando o nome padrão",
                    schema, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            cache.gravar(chaveCache, new NomeEmCache(NOME_PADRAO, true), VALIDADE_FALHA);
            return NOME_PADRAO;
        }
    }

    @Override
    public boolean aceitaNovosCadastros(String centroId) {
        try {
            CentroResposta resposta = circuitBreaker.executeSupplier(() -> consultar(centroId));
            return resposta == null || !Boolean.FALSE.equals(resposta.ativo());
        } catch (RuntimeException e) {
            if (naoEncontrado(e)) {
                return false;
            }
            log.warn("Não foi possível confirmar a situação do centro {} - causa: {}. Aceitando o cadastro",
                    centroId, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            return true;
        }
    }

    private static boolean naoEncontrado(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof org.springframework.web.client.HttpClientErrorException.NotFound) {
                return true;
            }
        }
        return false;
    }

    private CentroResposta consultar(String centroId) {
        return client.get()
                .uri("/internal/centros/{id}", centroId)
                .header(HEADER_CHAVE, chave)
                .retrieve()
                .body(CentroResposta.class);
    }

    private String buscar(String centroId) {
        CentroResposta resposta = client.get()
                .uri("/internal/centros/{id}", centroId)
                .header(HEADER_CHAVE, chave)
                .retrieve()
                .body(CentroResposta.class);
        if (resposta == null || resposta.nome() == null || resposta.nome().isBlank()) {
            throw new IllegalStateException("Resposta do Centro sem nome");
        }
        return resposta.nome();
    }
}
