package com.bloodlink.api.doador.application.service;


import com.bloodlink.api.doador.application.port.out.Impedimento;
import com.bloodlink.api.doador.application.port.out.ResultadoElegibilidade;
import com.bloodlink.api.doador.domain.MotivoInelegibilidade;
import com.bloodlink.api.doador.domain.SexoBiologico;
import com.bloodlink.api.doador.domain.StatusElegibilidade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Regra de elegibilidade para doar sangue, sem dependência de banco ou Spring.
 * Faixa de idade 16 a 69 anos; quem tem 60 ou mais só doa se já doou antes dos 60; peso mínimo de 50 kg;
 * intervalo mínimo de 60 dias (homens) ou 90 dias (mulheres); limite de 4 (homens) ou 3 (mulheres) doações em 12 meses.
 */

@Component
public class CalculadoraElegibilidade {

    public static final int IDADE_MINIMA = 16;
    public static final int IDADE_MAXIMA = 69;
    public static final int IDADE_LIMITE_PRIMEIRA_DOACAO = 60;
    public static final BigDecimal PESO_MINIMO_KG = new BigDecimal("50");

    public record Entrada(
            LocalDate nascimento,
            BigDecimal pesoKg,
            int intervaloMinimoDias,
            int maxDoacoesPorAno,
            List<Impedimento> impedimentos,
            List<LocalDate> datasDeDoacoes
    ) {
        public Entrada {
            if (nascimento == null) {
                throw new IllegalArgumentException("O nascimento é obrigatório para avaliar a elegibilidade");
            }
            if (intervaloMinimoDias <= 0 || maxDoacoesPorAno <= 0) {
                throw new IllegalArgumentException("Intervalo e limite anual devem ser positivos");
            }
            impedimentos = impedimentos == null ? List.of() : List.copyOf(impedimentos);
            datasDeDoacoes = datasDeDoacoes == null ? List.of() : List.copyOf(datasDeDoacoes);
        }
    }

    private CalculadoraElegibilidade() { }
    public static ResultadoElegibilidade avaliar(Entrada e, LocalDate hoje) {
        List<LocalDate> doacoes = e.datasDeDoacoes().stream()
                .filter(d -> !d.isAfter(hoje))
                .sorted()
                .toList();

        List<MotivoInelegibilidade> impedido = new ArrayList<>();
        LocalDate fimDosTemporarios = null;
        boolean soTemporarios = true;

        int idade = Period.between(e.nascimento(), hoje).getYears();
        if (idade < IDADE_MINIMA || idade > IDADE_MAXIMA) {
            impedido.add(MotivoInelegibilidade.IDADE_FORA_DA_FAIXA);
            soTemporarios = false;
        } else if (idade >= IDADE_LIMITE_PRIMEIRA_DOACAO && !doouAntesDosSessenta(e.nascimento(), doacoes)) {
            impedido.add(MotivoInelegibilidade.PRIMEIRA_DOACAO_APOS_60_ANOS);
            soTemporarios = false;
        }

        if (e.pesoKg() != null && e.pesoKg().compareTo(PESO_MINIMO_KG) < 0) {
            impedido.add(MotivoInelegibilidade.PESO_INSUFICIENTE);
            soTemporarios = false;
        }

        for (Impedimento imp : e.impedimentos()) {
            if (!imp.vigenteEm(hoje)) {
                continue;
            }
            if (imp.definitivo()) {
                if (!impedido.contains(MotivoInelegibilidade.IMPEDIMENTO_DEFINITIVO)) {
                    impedido.add(MotivoInelegibilidade.IMPEDIMENTO_DEFINITIVO);
                }
                soTemporarios = false;
            } else {
                if (!impedido.contains(MotivoInelegibilidade.IMPEDIMENTO_TEMPORARIO)) {
                    impedido.add(MotivoInelegibilidade.IMPEDIMENTO_TEMPORARIO);
                }
                LocalDate livre = imp.fim().plusDays(1);
                if (fimDosTemporarios == null || livre.isAfter(fimDosTemporarios)) {
                    fimDosTemporarios = livre;
                }
            }
        }

        if (!impedido.isEmpty()) {
            LocalDate previsao = soTemporarios ? fimDosTemporarios : null;
            return new ResultadoElegibilidade(StatusElegibilidade.IMPEDIDO, impedido, previsao);
        }

        List<MotivoInelegibilidade> aguardando = new ArrayList<>();
        LocalDate elegivelEm = null;

        if (e.pesoKg() == null) {
            aguardando.add(MotivoInelegibilidade.PESO_NAO_INFORMADO);
        }

        if (!doacoes.isEmpty()) {
            LocalDate ultima = doacoes.get(doacoes.size() - 1);
            LocalDate porIntervalo = ultima.plusDays(e.intervaloMinimoDias()); // ALTERADO
            if (hoje.isBefore(porIntervalo)) {
                aguardando.add(MotivoInelegibilidade.INTERVALO_MINIMO);
                elegivelEm = porIntervalo;
            }

            // janela de 12 meses: uma doação em "d" sai da janela em d + 1 ano
            List<LocalDate> naJanela = doacoes.stream().filter(d -> d.isAfter(hoje.minusYears(1))).toList();
            int limite = e.maxDoacoesPorAno(); // ALTERADO
            if (naJanela.size() >= limite) {
                LocalDate porLimite = naJanela.get(naJanela.size() - limite).plusYears(1);
                aguardando.add(MotivoInelegibilidade.LIMITE_ANUAL);
                if (elegivelEm == null || porLimite.isAfter(elegivelEm)) {
                    elegivelEm = porLimite;
                }
            }
        }

        if (!aguardando.isEmpty()) {
            return new ResultadoElegibilidade(StatusElegibilidade.AGUARDANDO, aguardando, elegivelEm);
        }
        return new ResultadoElegibilidade(StatusElegibilidade.ELEGIVEL, List.of(), null);
    }

    private static boolean doouAntesDosSessenta(LocalDate nascimento, List<LocalDate> doacoesOrdenadas) {
        LocalDate aos60 = nascimento.plusYears(IDADE_LIMITE_PRIMEIRA_DOACAO);
        return doacoesOrdenadas.stream().min(Comparator.naturalOrder()).map(d -> d.isBefore(aos60)).orElse(false);
    }
}