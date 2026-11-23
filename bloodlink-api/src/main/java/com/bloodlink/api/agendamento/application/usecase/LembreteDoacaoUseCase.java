package com.bloodlink.api.agendamento.application.usecase;

public interface LembreteDoacaoUseCase {

    /** Envia o lembrete dos agendamentos confirmados de hoje, no tenant atual. */
    ResultadoLembretes enviarLembretesDeHoje();

    /** semEmail: doador sem e-mail cadastrado. falhas: não foi possível publicar (ficam pendentes para a próxima execução). */
    record ResultadoLembretes(int enviados, int semEmail, int falhas) {
    }
}
