package com.bloodlink.api.agendamento.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConfiguracaoAgendamento {

    private static final Map<String, DayOfWeek> DIAS = Map.of(
            "SEG", DayOfWeek.MONDAY, "TER", DayOfWeek.TUESDAY, "QUA", DayOfWeek.WEDNESDAY,
            "QUI", DayOfWeek.THURSDAY, "SEX", DayOfWeek.FRIDAY, "SAB", DayOfWeek.SATURDAY,
            "DOM", DayOfWeek.SUNDAY);
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<LocalTime> horarios;
    private final int vagasPorHorario;
    private final int antecedenciaMinimaDias;
    private final int janelaMaximaDias;
    private final Set<DayOfWeek> diasAtendimento;
    private final Clock clock;

    public ConfiguracaoAgendamento(
            @Value("${bloodlink.agendamento.horarios:08:00,09:00,10:00,11:00,13:00,14:00,15:00,16:00}") String horarios,
            @Value("${bloodlink.agendamento.vagas-por-horario:3}") int vagasPorHorario,
            @Value("${bloodlink.agendamento.antecedencia-minima-dias:1}") int antecedenciaMinimaDias,
            @Value("${bloodlink.agendamento.janela-maxima-dias:60}") int janelaMaximaDias,
            @Value("${bloodlink.agendamento.dias-atendimento:SEG,TER,QUA,QUI,SEX}") String dias,
            Clock clock) {
        this.horarios = Arrays.stream(horarios.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(LocalTime::parse).distinct().sorted().toList();
        this.diasAtendimento = Arrays.stream(dias.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> {
                    DayOfWeek dia = DIAS.get(s.toUpperCase());
                    if (dia == null) {
                        throw new IllegalStateException("Dia de atendimento inválido na configuração: " + s);
                    }
                    return dia;
                })
                .collect(Collectors.toUnmodifiableSet());
        if (this.horarios.isEmpty() || this.diasAtendimento.isEmpty()) {
            throw new IllegalStateException("A configuração de agendamento precisa de horários e dias de atendimento");
        }
        if (vagasPorHorario < 1 || antecedenciaMinimaDias < 0 || janelaMaximaDias < antecedenciaMinimaDias) {
            throw new IllegalStateException("Configuração de agendamento inválida (vagas, antecedência ou janela)");
        }
        this.vagasPorHorario = vagasPorHorario;
        this.antecedenciaMinimaDias = antecedenciaMinimaDias;
        this.janelaMaximaDias = janelaMaximaDias;
        this.clock = clock;
    }

    public List<LocalTime> horarios() {
        return horarios;
    }

    public int vagasPorHorario() {
        return vagasPorHorario;
    }

    public boolean atende(LocalDate data) {
        return diasAtendimento.contains(data.getDayOfWeek());
    }

    /** A data precisa estar entre a antecedência mínima e a janela máxima. */
    public void validarData(LocalDate data) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate primeiro = hoje.plusDays(antecedenciaMinimaDias);
        LocalDate ultimo = hoje.plusDays(janelaMaximaDias);
        if (data.isBefore(primeiro) || data.isAfter(ultimo)) {
            throw new IllegalArgumentException(
                    "Escolha uma data entre " + primeiro.format(DATA_BR) + " e " + ultimo.format(DATA_BR));
        }
    }

    /** Para a equipe: de hoje até a janela máxima, sem a antecedência mínima (encaixe no balcão). */
    public void validarDataParaEquipe(LocalDate data) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate ultimo = hoje.plusDays(janelaMaximaDias);
        if (data.isBefore(hoje) || data.isAfter(ultimo)) {
            throw new IllegalArgumentException(
                    "Escolha uma data entre " + hoje.format(DATA_BR) + " e " + ultimo.format(DATA_BR));
        }
    }
}