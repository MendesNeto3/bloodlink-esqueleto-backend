package com.bloodlink.api.painel.service;

import com.bloodlink.api.agendamento.domain.StatusAgendamento;
import com.bloodlink.api.doador.adapter.out.persistence.DoacaoRepositoryPort;
import com.bloodlink.api.doador.adapter.out.persistence.ImpedimentoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.out.DoadorResumo;
import com.bloodlink.api.doador.application.port.out.Impedimento;
import com.bloodlink.api.doador.application.port.out.ResultadoElegibilidade;
import com.bloodlink.api.doador.application.service.AvaliadorElegibilidade;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.painel.PainelRepositoryPort;
import com.bloodlink.api.painel.domain.PainelResumo;
import com.bloodlink.api.painel.usecase.PainelUseCase;
import com.bloodlink.api.shared.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PainelService implements PainelUseCase {

    private static final Logger log = LoggerFactory.getLogger(PainelService.class);
    private static final List<StatusAgendamento> ATIVOS =
            List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO);
    private static final int LIMITE_MAXIMO_RECENTES = 20;
    private static final int PROXIMAS_COLETAS = 5;

    private final PainelRepositoryPort painel;
    private final DoadorRepositoryPort doadores;
    private final DoacaoRepositoryPort doacoes;
    private final ImpedimentoRepositoryPort impedimentos;
    private final AvaliadorElegibilidade avaliador;
    private final Clock clock;

    public PainelService(PainelRepositoryPort painel, DoadorRepositoryPort doadores,
                         DoacaoRepositoryPort doacoes, ImpedimentoRepositoryPort impedimentos,
                         AvaliadorElegibilidade avaliador, Clock clock) {
        this.painel = painel;
        this.doadores = doadores;
        this.doacoes = doacoes;
        this.impedimentos = impedimentos;
        this.avaliador = avaliador;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PainelResumo obter(YearMonth mes, int limiteRecentes) {
        YearMonth mesEscolhido = mes != null ? mes : YearMonth.now(clock);
        int limite = Math.max(1, Math.min(limiteRecentes, LIMITE_MAXIMO_RECENTES));
        try {
            LocalDate hoje = LocalDate.now(clock);

            long doacoesHoje = painel.contarDoacoes(hoje, hoje);
            long ultimos7 = painel.contarDoacoes(hoje.minusDays(6), hoje);
            long anteriores7 = painel.contarDoacoes(hoje.minusDays(13), hoje.minusDays(7));
            Double variacao = anteriores7 == 0 ? null
                    : Math.round((ultimos7 - anteriores7) * 1000.0 / anteriores7) / 10.0;
            long doacoesMes = painel.contarDoacoes(mesEscolhido.atDay(1), mesEscolhido.atEndOfMonth());

            LocalDate desde = hoje.minusMonths(12);
            var doadoresCont = new PainelResumo.Doadores(
                    painel.contarDoadoresComDoacaoDesde(desde),
                    painel.contarDoadoresRecorrentesDesde(desde));

            var agendamentos = new PainelResumo.Agendamentos(
                    painel.contarAgendamentos(hoje, hoje.plusDays(6), ATIVOS),
                    painel.contarAgendamentos(hoje, hoje, List.of(StatusAgendamento.CONFIRMADO)),
                    painel.contarAgendamentos(hoje, hoje.plusYears(1), List.of(StatusAgendamento.PENDENTE)));

            var coletas = new PainelResumo.Coletas(
                    mesEscolhido,
                    painel.diasComColeta(mesEscolhido.atDay(1), mesEscolhido.atEndOfMonth()),
                    painel.proximasColetas(hoje, PROXIMAS_COLETAS));

            return new PainelResumo(hoje,
                    new PainelResumo.Doacoes(doacoesHoje, ultimos7, anteriores7, variacao, doacoesMes),
                    doadoresCont, agendamentos, coletas, recentes(limite));
        } catch (RuntimeException e) {
            log.error("Falha ao montar o painel - causa: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("carregar o painel");
        }
    }

    private List<DoadorResumo> recentes(int limite) {
        List<UUID> ids = painel.idsDoadoresRecentes(limite);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Doador> porId = doadores.buscarPorIds(new HashSet<>(ids));
        Map<UUID, List<LocalDate>> datas = doacoes.datasPorDoadores(ids);
        Map<UUID, List<Impedimento>> imps = impedimentos.naoEncerradosPorDoadores(ids);

        return ids.stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .map(d -> montar(d, datas.getOrDefault(d.id(), List.of()), imps.getOrDefault(d.id(), List.of())))
                .toList();
    }

    private DoadorResumo montar(Doador d, List<LocalDate> datas, List<Impedimento> imps) {
        ResultadoElegibilidade r = avaliador.avaliar(d, datas, imps);
        LocalDate ultima = datas.isEmpty() ? null : Collections.max(datas);
        return new DoadorResumo(d.id(), d.nome(), d.tipoSanguineo(), d.ativo(),
                ultima, datas.size(), r.status(), r.elegivelEm());
    }
}