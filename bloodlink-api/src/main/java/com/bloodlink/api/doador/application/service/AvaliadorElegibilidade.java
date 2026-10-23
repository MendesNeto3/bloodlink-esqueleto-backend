package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.out.Impedimento;
import com.bloodlink.api.doador.application.port.out.ResultadoElegibilidade;
import com.bloodlink.api.doador.domain.Doador;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class AvaliadorElegibilidade {

    private final Clock clock;

    public AvaliadorElegibilidade(Clock clock) {
        this.clock = clock;
    }

    /** Avalia o doador como ele está hoje. */
    public ResultadoElegibilidade avaliar(Doador doador, List<LocalDate> datasDeDoacoes,
                                          List<Impedimento> impedimentos) {
        return avaliarEm(doador, datasDeDoacoes, impedimentos, LocalDate.now(clock));
    }

    /** Avalia como o doador estará em "dia" (usado para validar um agendamento futuro). */
    public ResultadoElegibilidade avaliarEm(Doador doador, List<LocalDate> datasDeDoacoes,
                                            List<Impedimento> impedimentos, LocalDate dia) {
        var entrada = new CalculadoraElegibilidade.Entrada(
                doador.dataNascimento(),
                doador.pesoKg(),
                doador.sexo().intervaloMinimoDias(),
                doador.sexo().maxDoacoesPorAno(),
                impedimentos,
                comDeclarada(doador, datasDeDoacoes));
        return CalculadoraElegibilidade.avaliar(entrada, dia);
    }

    /** A última doação declarada no autocadastro conta como uma data a mais (ainda não confirmada). */
    private static List<LocalDate> comDeclarada(Doador doador, List<LocalDate> datas) {
        List<LocalDate> base = datas == null ? List.of() : datas;
        LocalDate declarada = doador.ultimaDoacaoDeclarada();
        if (declarada == null || base.contains(declarada)) {
            return base;
        }
        List<LocalDate> todas = new ArrayList<>(base);
        todas.add(declarada);
        return todas;
    }
}