package com.bloodlink.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CacheJsonTest {

    private static final TypeReference<Map<String, Integer>> MAPA = new TypeReference<>() { };
    private static final Duration TTL = Duration.ofMinutes(5);

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valores;

    private CacheJson cache;

    @BeforeEach
    void preparar() {
        when(redis.opsForValue()).thenReturn(valores);
        cache = new CacheJson(redis, new ObjectMapper());
    }

    @AfterEach
    void limparTransacao() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("ler: devolve o objeto guardado; ausente ou JSON quebrado vira vazio")
    void ler() {
        when(valores.get("k")).thenReturn("{\"a\":1}");
        assertThat(cache.ler("k", MAPA)).contains(Map.of("a", 1));

        when(valores.get("vazio")).thenReturn(null);
        assertThat(cache.ler("vazio", MAPA)).isEmpty();

        when(valores.get("quebrado")).thenReturn("{nao e json");
        assertThat(cache.ler("quebrado", MAPA)).isEmpty();
    }

    @Test
    @DisplayName("ler: Redis fora do ar não propaga exceção")
    void lerRedisFora() {
        when(valores.get(anyString())).thenThrow(new RuntimeException("redis fora"));

        assertThat(cache.ler("k", MAPA)).isEmpty();
    }

    @Test
    @DisplayName("gravar: guarda o JSON com o TTL; falha é engolida")
    void gravar() {
        cache.gravar("k", Map.of("a", 1), TTL);
        verify(valores).set("k", "{\"a\":1}", TTL);

        doAnswer(i -> { throw new RuntimeException("redis fora"); }).when(valores).set(anyString(), anyString(), any(Duration.class));
        assertThatCode(() -> cache.gravar("k", Map.of("a", 1), TTL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("apagar: remove as chaves; falha é engolida")
    void apagar() {
        cache.apagar("a", "b");
        verify(redis).delete(List.of("a", "b"));

        when(redis.delete(any(java.util.Collection.class))).thenThrow(new RuntimeException("redis fora"));
        assertThatCode(() -> cache.apagar("a")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("apagarAposCommit sem transação apaga na hora")
    void semTransacao() {
        cache.apagarAposCommit("a");

        verify(redis).delete(List.of("a"));
    }

    @Test
    @DisplayName("apagarAposCommit dentro de transação só apaga depois do commit")
    void comTransacao() {
        TransactionSynchronizationManager.initSynchronization();

        cache.apagarAposCommit("a");

        verify(redis, never()).delete(any(java.util.Collection.class));
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.afterCommit();
        }
        verify(redis).delete(List.of("a"));
    }

    @Test
    @DisplayName("obterOu: cache cheio não consulta a origem")
    void obterOuHit() {
        when(valores.get("k")).thenReturn("{\"a\":1}");
        AtomicInteger chamadas = new AtomicInteger();

        Map<String, Integer> r = cache.obterOu("k", TTL, MAPA, () -> {
            chamadas.incrementAndGet();
            return Map.of("b", 2);
        });

        assertThat(r).isEqualTo(Map.of("a", 1));
        assertThat(chamadas).hasValue(0);
    }

    @Test
    @DisplayName("obterOu: cache vazio busca na origem e guarda o resultado")
    void obterOuMiss() {
        when(valores.get("k")).thenReturn(null);

        Map<String, Integer> r = cache.obterOu("k", TTL, MAPA, () -> Map.of("b", 2));

        assertThat(r).isEqualTo(Map.of("b", 2));
        verify(valores).set("k", "{\"b\":2}", TTL);
    }

    @Test
    @DisplayName("obterOu: origem nula não é guardada; Redis fora ainda devolve a origem")
    void obterOuNuloEFalha() {
        when(valores.get("k")).thenReturn(null);
        assertThat(cache.<Map<String, Integer>>obterOu("k", TTL, MAPA, () -> null)).isNull();
        verify(valores, never()).set(anyString(), anyString(), any(Duration.class));

        when(valores.get("fora")).thenThrow(new RuntimeException("redis fora"));
        assertThat(cache.obterOu("fora", TTL, MAPA, () -> Map.of("c", 3))).isEqualTo(Map.of("c", 3));
    }

    @Test
    @DisplayName("apagarPorPadrao: usa SCAN e apaga o que achou; falha é engolida")
    @SuppressWarnings("unchecked")
    void porPadrao() {
        Cursor<String> cursor = org.mockito.Mockito.mock(Cursor.class);
        doAnswer(i -> {
            Consumer<String> c = i.getArgument(0);
            c.accept("x1");
            c.accept("x2");
            return null;
        }).when(cursor).forEachRemaining(any());
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);

        cache.apagarPorPadrao("bloodlink:painel:*");

        verify(redis).delete(List.of("x1", "x2"));

        when(redis.scan(any(ScanOptions.class))).thenThrow(new RuntimeException("redis fora"));
        assertThatCode(() -> cache.apagarPorPadrao("p:*")).doesNotThrowAnyException();
    }
}
