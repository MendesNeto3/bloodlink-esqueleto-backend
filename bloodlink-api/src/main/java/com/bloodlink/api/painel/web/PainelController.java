package com.bloodlink.api.painel.web;

import com.bloodlink.api.agendamento.application.port.out.CentroConsultaPort;
import com.bloodlink.api.painel.domain.PainelResumo;
import com.bloodlink.api.painel.usecase.PainelUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelUseCase useCase;
    private final CentroConsultaPort centros;

    public PainelController(PainelUseCase useCase, CentroConsultaPort centros) {
        this.useCase = useCase;
        this.centros = centros;
    }

    @GetMapping
    public PainelResumo obter(
            @RequestParam(name = "mes", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(name = "limiteRecentes", defaultValue = "5") int limiteRecentes) {
        return useCase.obter(mes, limiteRecentes).comCentro(centros.nomeDoCentroAtual());
    }
}