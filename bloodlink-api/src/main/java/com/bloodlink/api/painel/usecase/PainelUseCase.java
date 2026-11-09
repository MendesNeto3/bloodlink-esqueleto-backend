package com.bloodlink.api.painel.usecase;

import com.bloodlink.api.painel.domain.PainelResumo;

import java.time.YearMonth;

public interface PainelUseCase {

    PainelResumo obter(YearMonth mes, int limiteRecentes);
}
