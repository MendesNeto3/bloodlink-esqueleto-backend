package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.ConsultarDoadorUseCase;
import com.bloodlink.api.doador.application.port.in.ConsultarElegibilidadeUseCase;
import com.bloodlink.api.doador.application.port.in.DoacaoUseCase;
import com.bloodlink.api.doador.application.port.in.ObterResumoDoadorUseCase;
import com.bloodlink.api.doador.application.port.out.ResumoDoador;
import com.bloodlink.api.doador.domain.Doacao;
import com.bloodlink.api.doador.domain.Doador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ObterResumoDoadorService implements ObterResumoDoadorUseCase {

    private final ConsultarDoadorUseCase  consultarDoadorUseCase;
    private final DoacaoUseCase doacaoUseCase;
    private final ConsultarElegibilidadeUseCase elegibilidadeUseCase;

    public ObterResumoDoadorService (ConsultarDoadorUseCase consultarDoadorUseCase,
                                     DoacaoUseCase doacaoUseCase,
                                     ConsultarElegibilidadeUseCase elegibilidadeUseCase) {
        this.consultarDoadorUseCase = consultarDoadorUseCase;
        this.doacaoUseCase = doacaoUseCase;
        this.elegibilidadeUseCase = elegibilidadeUseCase;
    }

    @Override
    public ResumoDoador obter(UUID doadorId) {
        if (doadorId == null){
            throw new IllegalArgumentException("O id do doador é obrigatório");
        }

        Doador doador = consultarDoadorUseCase.porId(doadorId);
        List<Doacao> historico = doacaoUseCase.historico(doadorId);

        LocalDate ultimaRegistrada = historico.stream()
                .map(Doacao::data)
                .max(Comparator.naturalOrder())
                .orElse(null);

        LocalDate ultima = maisRecente(ultimaRegistrada, doador.ultimaDoacaoDeclarada());

        return new ResumoDoador(doador, historico.size(), ultima, elegibilidadeUseCase.consultar(doadorId));
    }

    private static LocalDate maisRecente(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
