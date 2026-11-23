package com.bloodlink.common.cache;

import com.bloodlink.common.log.MascaraLog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cache em Redis com valores em JSON. Cache é só otimização: qualquer falha (Redis fora do ar,
 * timeout, JSON de uma versão antiga do objeto) é tratada como "não achei" e a requisição segue
 * pela origem. Nenhum método lança exceção.
 */
public class CacheJson {

    private static final Logger log = LoggerFactory.getLogger(CacheJson.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public CacheJson(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public <T> Optional<T> ler(String chave, TypeReference<T> tipo) {
        try {
            String json = redis.opsForValue().get(chave);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(mapper.readValue(json, tipo));
        } catch (Exception e) {
            log.warn("Cache indisponível na leitura da chave {} - causa: {}", chave, MascaraLog.causa(e));
            return Optional.empty();
        }
    }

    public void gravar(String chave, Object valor, Duration ttl) {
        try {
            redis.opsForValue().set(chave, mapper.writeValueAsString(valor), ttl);
        } catch (Exception e) {
            log.warn("Cache indisponível na gravação da chave {} - causa: {}", chave, MascaraLog.causa(e));
        }
    }

    public void apagar(String... chaves) {
        try {
            redis.delete(Arrays.asList(chaves));
        } catch (Exception e) {
            log.warn("Cache indisponível ao apagar chaves - causa: {}", MascaraLog.causa(e));
        }
    }

    /**
     * Apaga depois que a transação atual confirmar (se não houver transação, apaga já). Apagar antes do
     * commit deixaria outra requisição recolocar no cache o valor antigo, ainda visível no banco.
     */
    public void apagarAposCommit(String... chaves) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    apagar(chaves);
                }
            });
        } else {
            apagar(chaves);
        }
    }

    /** Apaga as chaves que casam com o padrão (ex.: "bloodlink:painel:centro_abc:*"), usando SCAN (nunca KEYS). */
    public void apagarPorPadrao(String padrao) {
        try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match(padrao).count(100).build())) {
            List<String> achadas = new ArrayList<>();
            cursor.forEachRemaining(achadas::add);
            if (!achadas.isEmpty()) {
                redis.delete(achadas);
            }
        } catch (Exception e) {
            log.warn("Cache indisponível ao apagar por padrão - causa: {}", MascaraLog.causa(e));
        }
    }

    /** Devolve o valor do cache; se não houver, busca na origem, guarda e devolve. Valor nulo não é guardado. */
    public <T> T obterOu(String chave, Duration ttl, TypeReference<T> tipo, Supplier<T> origem) {
        Optional<T> emCache = ler(chave, tipo);
        if (emCache.isPresent()) {
            return emCache.get();
        }
        T valor = origem.get();
        if (valor != null) {
            gravar(chave, valor, ttl);
        }
        return valor;
    }
}
