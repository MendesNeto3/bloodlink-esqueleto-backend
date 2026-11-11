package com.bloodlink.estoque.application.service;

import com.bloodlink.common.log.MascaraLog;
import com.bloodlink.estoque.application.exception.OperacaoEstoqueException;
import com.bloodlink.estoque.application.exception.RecursoNaoEncontradoException;
import com.bloodlink.estoque.application.exception.RegraDeNegocioException;
import com.bloodlink.estoque.application.port.in.EstoqueUseCase;
import com.bloodlink.estoque.application.port.out.EstoqueEventosPort;
import com.bloodlink.estoque.application.port.out.EstoqueRepositoryPort;
import com.bloodlink.estoque.application.port.out.EstoqueRepositoryPort.LoteDisponivel;
import com.bloodlink.estoque.domain.Lote;
import com.bloodlink.estoque.domain.ResumoEstoque;
import com.bloodlink.estoque.domain.TipoSanguineo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class EstoqueService implements EstoqueUseCase {

    private static final Logger log = LoggerFactory.getLogger(EstoqueService.class);
    private static final int QUANTIDADE_MAXIMA = 1000;
    private static final int PARAMETRO_MAXIMO = 100_000;
    private static final int DIAS_ALERTA_VENCIMENTO = 7;

    private final EstoqueRepositoryPort estoque;
    private final EstoqueEventosPort eventos;
    private final Clock clock;
    private final int validadePadraoDias;

    public EstoqueService(EstoqueRepositoryPort estoque, EstoqueEventosPort eventos, Clock clock,
                          @Value("${bloodlink.estoque.validade-padrao-dias:42}") int validadePadraoDias) {
        this.estoque = estoque;
        this.eventos = eventos;
        this.clock = clock;
        this.validadePadraoDias = validadePadraoDias;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoEstoque resumo() {
        return executar("consultar o estoque", () -> {
            LocalDate hoje = LocalDate.now(clock);
            return ResumoEstoque.de(estoque.situacaoPorTipo(hoje, hoje.plusDays(DIAS_ALERTA_VENCIMENTO)));
        });
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
    public Lote registrarEntrada(EntradaLoteComando c) {
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
        LocalDate hoje = LocalDate.now(clock);
        if (c.dataColeta().isAfter(hoje)) {
            throw new IllegalArgumentException("A data da coleta não pode ser futura");
        }
        LocalDate validade = c.dataValidade() != null ? c.dataValidade() : c.dataColeta().plusDays(validadePadraoDias);
        if (validade.isBefore(c.dataColeta())) {
            throw new IllegalArgumentException("A validade não pode ser anterior à coleta");
        }
        if (validade.isBefore(hoje)) {
            throw new RegraDeNegocioException("Este lote já está vencido e não pode entrar no estoque");
        }
        return executar("registrar a entrada no estoque", () -> {
            Lote lote = new Lote(UUID.randomUUID(), tipo, quantidade, c.dataColeta(), validade, c.registradoPor());
            estoque.salvarLote(lote);
            log.info("Lote {} de {} ({} un) registrado por {}", lote.id(), tipo.name(), quantidade, c.registradoPor());
            return lote;
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
                estoque.atualizarQuantidadeLote(lote.id(), lote.quantidade() - retira);
                falta -= retira;
            }
            long restante = disponivel - quantidade;
            log.info("Baixa de {} un de {} registrada por {}", quantidade, tipo.name(), c.registradoPor());

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
            if (!estoque.atualizarParametros(tipo, limiarCritico, metaUnidades)) {
                throw new RecursoNaoEncontradoException("Tipo sanguíneo não encontrado no estoque");
            }
            log.info("Parâmetros de {} atualizados", tipo.name());
            return null;
        });
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