package com.bloodlink.api.agendamento.application.service;

import com.bloodlink.api.agendamento.application.usecase.LembreteDoacaoUseCase;
import com.bloodlink.api.agendamento.domain.SituacaoLembrete;

public final class Contagem {
    private int enviados;
    private int semEmail;
    private int falhas;

    public void registrar(SituacaoLembrete situacao) {
        switch (situacao) {
            case ENVIADO -> enviados++;
            case SEM_EMAIL -> semEmail++;
            case FALHA -> falhas++;
        }
    }

    LembreteDoacaoUseCase.ResultadoLembretes resultado() {
        return new LembreteDoacaoUseCase.ResultadoLembretes(enviados, semEmail, falhas);
    }
}
