package com.bloodlink.estoque.application.service;

import com.bloodlink.common.cache.CacheJson;
import com.bloodlink.common.log.MascaraLog;
import com.bloodlink.estoque.application.exception.OperacaoEstoqueException;
import com.bloodlink.estoque.application.exception.RecursoNaoEncontradoException;
import com.bloodlink.estoque.application.exception.RegraDeNegocioException;
import com.bloodlink.estoque.application.port.in.EstoqueUseCase;
import com.bloodlink.estoque.application.port.out.EstoqueEventosPort;
import com.bloodlink.estoque.application.port.out.EstoqueRepositoryPort;
import com.bloodlink.estoque.application.port.out.EstoqueRepositoryPort.LoteDisponivel;
import com.bloodlink.estoque.domain.Lote;
import com.bloodlink.estoque.domain.MotivoBaixa;
import com.bloodlink.estoque.domain.Movimentacao;
import com.bloodlink.estoque.domain.ResumoEstoque;
import com.bloodlink.estoque.domain.TipoSanguineo;
import com.bloodlink.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Service
public class EstoqueService implements EstoqueUseCase {

    private static final Logger log = LoggerFactory.getLogger(EstoqueService.class);
    private static final int QUANTIDADE_MAXIMA = 1000;
    private static final int PARAMETRO_MAXIMO = 100_000;
    private static final int CHAVE_TAMANHO_MAXIMO = 80;
    private static final Pattern CHAVE_VALIDA = Pattern.compile("[A-Za-z0-9._:-]+");
    private static final int MOVIMENTACOES_JANELA_PADRAO_DIAS = 30;
    private static final int MOVIMENTACOES_JANELA_MAXIMA_DIAS = 90;
    private static final int MOVIMENTACOES_LIMITE = 500;

    /** O resumo da tela de estoque (um group by sobre os lotes) fica 15 s no Redis e é apagado a cada escrita. */
    private static final Duration VALIDADE_RESUMO = Duration.ofSeconds(15);
    private static final TypeReference<ResumoEstoque> TIPO_RESUMO = new TypeReference<ResumoEstoque>() { };

    private final EstoqueRepositoryPort estoque;
    private final EstoqueEventosPort eventos;
    private final Clock clock;
    private final CacheJson cache;
    private final int validadePadraoDias;
    private final int validadeMaximaDias;
    private final int diasAlertaVencimento;

    public EstoqueService(EstoqueRepositoryPort estoque, EstoqueEventosPort eventos, Clock clock, CacheJson cache,
                          @Value("${bloodlink.estoque.validade-padrao-dias:42}") int validadePadraoDias,
                          @Value("${bloodlink.estoque.validade-maxima-dias:365}") int validadeMaximaDias,
                          @Value("${bloodlink.estoque.dias-alerta-vencimento:7}") int diasAlertaVencimento) {
        if (validadePadraoDias < 1 || validadeMaximaDias < validadePadraoDias || diasAlertaVencimento < 0) {
            throw new IllegalStateException("Configuração de validade do estoque inválida: "
                    + "validade-padrao-dias deve ser >= 1, validade-maxima-dias >= validade-padrao-dias "
                    + "e dias-alerta-vencimento >= 0");
        }
        this.estoque = estoque;
        this.eventos = eventos;
        this.clock = clock;
        this.cache = cache;
        this.validadePadraoDias = validadePadraoDias;
        this.validadeMaximaDias = validadeMaximaDias;
        this.diasAlertaVencimento = diasAlertaVencimento;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoEstoque resumo() {
        return cache.obterOu(chaveResumo(), VALIDADE_RESUMO, TIPO_RESUMO, () ->
                executar("consultar o estoque", () -> {
                    LocalDate hoje = LocalDate.now(clock);
                    return ResumoEstoque.de(estoque.situacaoPorTipo(hoje, hoje.plusDays(diasAlertaVencimento)));
                }));
    }

    private static String chaveResumo() {
        return "bloodlink:estoque:resumo:" + TenantContext.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> listarLotes(String tipoSanguineo, boolean incluirVencidos) {
        TipoSanguineo tipo = (tipoSanguineo == null || tipoSanguineo.isBlank()) ? null : tipo(tipoSanguineo);
        return executar("listar os lotes", () ->
                estoque.listarLotes(tipo, incluirVencidos ? null : LocalDate.now(clock)));
    }

    @Override
    @Transactional
    public ResultadoEntrada registrarEntrada(EntradaLoteComando c) {
        if (c == null) {
            throw new IllegalArgumentException("Os dados do lote são obrigatórios");
        }
        TipoSanguineo tipo = tipo(c.tipoSanguineo());
        int quantidade = quantidade(c.quantidadeUnidades());
        if (c.dataColeta() == null) {
            throw new IllegalArgumentException("A data da coleta é obrigatória");
        }
        if (c.registradoPor() == null) {
            throw new IllegalArgumentException("Não foi possível identificar quem registrou o lote");
        }
        String chave = chave(c.chaveIdempotencia());
        LocalDate hoje = LocalDate.now(clock);
        if (c.dataColeta().isAfter(hoje)) {
            throw new IllegalArgumentException("A data da coleta não pode ser futura");
        }
        LocalDate validade = c.dataValidade() != null ? c.dataValidade() : c.dataColeta().plusDays(validadePadraoDias);
        if (validade.isBefore(c.dataColeta())) {
            throw new IllegalArgumentException("A validade não pode ser anterior à coleta");
        }
        if (validade.isAfter(c.dataColeta().plusDays(validadeMaximaDias))) {
            throw new IllegalArgumentException("A validade não pode passar de " + validadeMaximaDias
                    + " dias após a coleta");
        }
        if (validade.isBefore(hoje)) {
            throw new RegraDeNegocioException("Este lote já está vencido e não pode entrar no estoque");
        }
        return executar("registrar a entrada no estoque", () -> {
            Lote lote = new Lote(UUID.randomUUID(), tipo, quantidade, c.dataColeta(), validade, c.registradoPor());
            // o histórico entra primeiro: é ele que barra a entrada repetida (chave única)
            if (!estoque.registrarMovimentacao(Movimentacao.entrada(lote, chave))) {
                return reaproveitar(chave, lote);
            }
            estoque.salvarLote(lote);
            cache.apagarAposCommit(chaveResumo());
            log.info("Lote {} de {} ({} un) registrado por {}", lote.id(), tipo.name(), quantidade, c.registradoPor());
            return new ResultadoEntrada(lote, true);
        });
    }

    /** Retira as unidades dos lotes que vencem primeiro (FEFO). Tudo na mesma transação, com os lotes travados. */
    @Override
    @Transactional
    public ResultadoBaixa darBaixa(BaixaComando c) {
        if (c == null) {
            throw new IllegalArgumentException("Os dados da baixa são obrigatórios");
        }
        TipoSanguineo tipo = tipo(c.tipoSanguineo());
        int quantidade = quantidade(c.quantidadeUnidades());
        MotivoBaixa motivo = motivo(c.motivo());
        if (c.registradoPor() == null) {
            throw new IllegalArgumentException("Não foi possível identificar quem registrou a baixa");
        }
        return executar("dar baixa no estoque", () -> {
            List<LoteDisponivel> lotes = estoque.bloquearLotesParaBaixa(tipo, LocalDate.now(clock));
            long disponivel = lotes.stream().mapToLong(LoteDisponivel::quantidade).sum();
            if (disponivel < quantidade) {
                throw new RegraDeNegocioException("Estoque insuficiente de " + tipo.rotulo()
                        + ": há " + disponivel + " unidade(s) válida(s)");
            }
            int falta = quantidade;
            for (LoteDisponivel lote : lotes) {
                if (falta == 0) {
                    break;
                }
                int retira = Math.min(falta, lote.quantidade());
                int sobra = lote.quantidade() - retira;
                if (sobra == 0) {
                    estoque.removerLote(lote.id());
                } else {
                    estoque.atualizarQuantidadeLote(lote.id(), sobra);
                }
                estoque.registrarMovimentacao(
                        Movimentacao.baixa(tipo, lote.id(), retira, motivo, c.registradoPor()));
                falta -= retira;
            }
            cache.apagarAposCommit(chaveResumo());
            long restante = disponivel - quantidade;
            log.info("Baixa de {} un de {} ({}) registrada por {}", quantidade, tipo.name(), motivo.name(),
                    c.registradoPor());

            // alerta só na passada de "acima do limiar" para "no limiar ou abaixo"
            int limiar = estoque.limiarCritico(tipo);
            if (disponivel > limiar && restante <= limiar) {
                eventos.estoqueCritico(tipo, restante, limiar);
            }
            return new ResultadoBaixa(tipo, quantidade, restante);
        });
    }

    @Override
    @Transactional
    public void atualizarParametros(String tipoSanguineo, Integer limiarCritico, Integer metaUnidades) {
        TipoSanguineo tipo = tipo(tipoSanguineo);
        if (limiarCritico == null || limiarCritico < 0 || limiarCritico > PARAMETRO_MAXIMO) {
            throw new IllegalArgumentException("O limiar crítico deve estar entre 0 e " + PARAMETRO_MAXIMO);
        }
        if (metaUnidades == null || metaUnidades < 1 || metaUnidades > PARAMETRO_MAXIMO) {
            throw new IllegalArgumentException("A meta deve estar entre 1 e " + PARAMETRO_MAXIMO);
        }
        executar("atualizar os parâmetros do estoque", () -> {
            int limiarAnterior = estoque.limiarCritico(tipo); // 404 se o tipo não existir
            if (!estoque.atualizarParametros(tipo, limiarCritico, metaUnidades)) {
                throw new RecursoNaoEncontradoException("Tipo sanguíneo não encontrado no estoque");
            }
            log.info("Parâmetros de {} atualizados", tipo.name());
            cache.apagarAposCommit(chaveResumo());

            // subir o limiar pode tornar crítico um saldo que antes não era
            long saldo = estoque.saldoDisponivel(tipo, LocalDate.now(clock));
            if (saldo > limiarAnterior && saldo <= limiarCritico) {
                eventos.estoqueCritico(tipo, saldo, limiarCritico);
            }
            return null;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Movimentacao> listarMovimentacoes(String tipoSanguineo, LocalDate de, LocalDate ate) {
        TipoSanguineo tipo = (tipoSanguineo == null || tipoSanguineo.isBlank()) ? null : tipo(tipoSanguineo);
        LocalDate fim = ate != null ? ate : LocalDate.now(clock);
        LocalDate inicio = de != null ? de : fim.minusDays(MOVIMENTACOES_JANELA_PADRAO_DIAS);
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > MOVIMENTACOES_JANELA_MAXIMA_DIAS) {
            throw new IllegalArgumentException("O período não pode passar de "
                    + MOVIMENTACOES_JANELA_MAXIMA_DIAS + " dias");
        }
        Instant desde = inicio.atStartOfDay(clock.getZone()).toInstant();
        Instant ateInstante = fim.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
        return executar("listar as movimentações", () ->
                estoque.listarMovimentacoes(tipo, desde, ateInstante, MOVIMENTACOES_LIMITE));
    }

    /** A chave já foi usada: devolve o lote original se for a mesma entrada, senão recusa. */
    private ResultadoEntrada reaproveitar(String chave, Lote pedido) {
        Movimentacao anterior = estoque.buscarEntradaPorChave(chave).orElseThrow(() ->
                new RegraDeNegocioException("Conflito na chave de idempotência, tente novamente"));
        if (anterior.tipoSanguineo() != pedido.tipoSanguineo()
                || anterior.quantidade() != pedido.quantidadeUnidades()) {
            throw new RegraDeNegocioException("Esta chave de idempotência já foi usada em outra entrada");
        }
        Lote existente = estoque.buscarLote(anterior.loteId()).orElseThrow(() ->
                new RegraDeNegocioException("Esta entrada já foi registrada e o lote já foi totalmente utilizado"));
        if (!existente.dataColeta().equals(pedido.dataColeta())
                || !existente.dataValidade().equals(pedido.dataValidade())) {
            throw new RegraDeNegocioException("Esta chave de idempotência já foi usada em outra entrada");
        }
        return new ResultadoEntrada(existente, false);
    }

    private static TipoSanguineo tipo(String texto) {
        return TipoSanguineo.de(texto).orElseThrow(() -> new IllegalArgumentException(
                "Tipo sanguíneo inválido: use A_POSITIVO, A_NEGATIVO, B_POSITIVO, B_NEGATIVO, AB_POSITIVO, "
                        + "AB_NEGATIVO, O_POSITIVO ou O_NEGATIVO (ou o rótulo, como O-)"));
    }

    private static int quantidade(Integer valor) {
        if (valor == null || valor < 1 || valor > QUANTIDADE_MAXIMA) {
            throw new IllegalArgumentException("A quantidade deve estar entre 1 e " + QUANTIDADE_MAXIMA + " unidades");
        }
        return valor;
    }

    /** Sem motivo informado, a baixa é registrada como OUTRO. */
    private static MotivoBaixa motivo(String texto) {
        if (texto == null || texto.isBlank()) {
            return MotivoBaixa.OUTRO;
        }
        return MotivoBaixa.de(texto).orElseThrow(() -> new IllegalArgumentException(
                "Motivo inválido: use TRANSFUSAO, DESCARTE ou OUTRO"));
    }

    private static String chave(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpa = texto.trim();
        if (limpa.length() > CHAVE_TAMANHO_MAXIMO || !CHAVE_VALIDA.matcher(limpa).matches()) {
            throw new IllegalArgumentException("A chave de idempotência deve ter até " + CHAVE_TAMANHO_MAXIMO
                    + " caracteres, apenas letras, números, ponto, hífen, dois-pontos e sublinhado");
        }
        return limpa;
    }

    private <T> T executar(String operacao, Supplier<T> acao) {
        try {
            return acao.get();
        } catch (RegraDeNegocioException | RecursoNaoEncontradoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao {} - causa: {}", operacao, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoEstoqueException(operacao);
        }
    }
}