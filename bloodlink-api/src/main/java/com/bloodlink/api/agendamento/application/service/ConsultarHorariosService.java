package com.bloodlink.api.agendamento.application.service;

import com.bloodlink.api.agendamento.application.port.in.AgendamentoRepositoryPort;
import com.bloodlink.api.agendamento.application.usecase.ConsultarHorarioUseCase;
import com.bloodlink.api.agendamento.domain.HorarioComVagas;
import com.bloodlink.api.agendamento.domain.HorarioDisponivel;
import com.bloodlink.api.agendamento.domain.HorariosDaEquipe;
import com.bloodlink.api.agendamento.domain.HorariosDoDia;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.shared.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class ConsultarHorariosService implements ConsultarHorarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultarHorariosService.class);

    private final AgendamentoRepositoryPort agendamentos;
    private final ConfiguracaoAgendamento configuracao;

    public ConsultarHorariosService(AgendamentoRepositoryPort agendamentos, ConfiguracaoAgendamento configuracao) {
        this.agendamentos = agendamentos;
        this.configuracao = configuracao;
    }

    @Override
    public HorariosDoDia consultar(LocalDate data) {
        exigirData(data);
        configuracao.validarData(data);
        if (!configuracao.atende(data)) {
            return new HorariosDoDia(data, List.of());
        }
        Map<LocalTime, Long> ocupados = ocupadosPorHorario(data);
        List<HorarioDisponivel> horarios = configuracao.horarios().stream()
                .map(h -> new HorarioDisponivel(h, ocupados.getOrDefault(h, 0L) < configuracao.vagasPorHorario()))
                .toList();
        return new HorariosDoDia(data, horarios);
    }

    @Override
    public HorariosDaEquipe consultarParaEquipe(LocalDate data) {
        exigirData(data);
        configuracao.validarDataParaEquipe(data);
        if (!configuracao.atende(data)) {
            return new HorariosDaEquipe(data, List.of());
        }
        Map<LocalTime, Long> ocupados = ocupadosPorHorario(data);
        List<HorarioComVagas> horarios = configuracao.horarios().stream()
                .map(h -> new HorarioComVagas(h, configuracao.vagasPorHorario(), ocupados.getOrDefault(h, 0L)))
                .toList();
        return new HorariosDaEquipe(data, horarios);
    }

    private static void exigirData(LocalDate data) {
        if (data == null) {
            throw new IllegalArgumentException("A data é obrigatória");
        }
    }

    private Map<LocalTime, Long> ocupadosPorHorario(LocalDate data) {
        try {
            return agendamentos.contarAtivosPorHorario(data);
        } catch (RuntimeException e) {
            log.error("Falha ao consultar horários de {} - causa: {}", data, MascaraLog.causa(e), e);
            throw new OperacaoDoadorException("consultar os horários");
        }
    }
}