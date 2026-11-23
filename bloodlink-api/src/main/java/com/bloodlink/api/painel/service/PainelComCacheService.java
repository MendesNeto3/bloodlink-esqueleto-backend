package com.bloodlink.api.painel.service;

import com.bloodlink.api.painel.domain.PainelResumo;
import com.bloodlink.api.painel.usecase.PainelUseCase;
import com.bloodlink.common.cache.CacheJson;
import com.bloodlink.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.YearMonth;

/**
 * Coloca o painel em cache (Redis) por centro, mês e limite. Fica na frente do {@link PainelService}: no cache,
 * a requisição nem abre transação no banco. Os números do painel podem ficar até 1 minuto defasados.
 * O resumo traz nomes de doadores: ficam no Redis só pelo TTL, e a senha do Redis protege o acesso.
 */
@Primary
@Service
public class PainelComCacheService implements PainelUseCase {

    private static final Duration VALIDADE = Duration.ofSeconds(60);
    private static final TypeReference<PainelResumo> TIPO = new TypeReference<PainelResumo>() { };

    private final PainelService painel;
    private final CacheJson cache;

    public PainelComCacheService(PainelService painel, CacheJson cache) {
        this.painel = painel;
        this.cache = cache;
    }

    @Override
    public PainelResumo obter(YearMonth mes, int limiteRecentes) {
        // mesmo limite que o PainelService aplica: valores diferentes que dão no mesmo resultado dividem a mesma chave
        int limite = Math.max(1, Math.min(limiteRecentes, PainelService.LIMITE_MAXIMO_RECENTES));
        String chave = "bloodlink:painel:" + TenantContext.get() + ":" + (mes == null ? "atual" : mes) + ":" + limite;
        return cache.obterOu(chave, VALIDADE, TIPO, () -> painel.obter(mes, limite));
    }
}
