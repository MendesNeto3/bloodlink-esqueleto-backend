package com.bloodlink.api.painel.domain;

import com.bloodlink.api.doador.application.port.out.DoadorResumo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record PainelResumo(LocalDate hoje,
                           Doacoes doacoes,
                           Doadores doadores,
                           Agendamentos agendamentos,
                           Coletas coletas,
                           List<DoadorResumo> doadoresRecentes) {

    public record Doacoes(long hoje, long ultimos7Dias, long anteriores7Dias,
                          Double variacaoPercentual, long mes) {
    }

    public record Doadores(long ativos, long recorrentes) {
    }

    public record Agendamentos(long proximos7Dias, long confirmadosHoje, long pendentes) {
    }

    public record Coletas(YearMonth mes, List<Integer> diasComColeta, List<ProximaColeta> proximas) {
    }

    public record ProximaColeta(UUID agendamentoId, UUID doadorId, String doadorNome,
                                LocalDate data, LocalTime hora) {
    }
}