package com.bloodlink.api.painel;

import com.bloodlink.api.agendamento.domain.StatusAgendamento;
import com.bloodlink.api.painel.domain.PainelResumo;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PainelRepositoryPort {

    long contarDoacoes(LocalDate inicio, LocalDate fim);

    long contarDoadoresComDoacaoDesde(LocalDate desde);

    long contarDoadoresRecorrentesDesde(LocalDate desde);

    long contarAgendamentos(LocalDate inicio, LocalDate fim, Collection<StatusAgendamento> status);

    List<Integer> diasComColeta(LocalDate inicio, LocalDate fim);

    List<PainelResumo.ProximaColeta> proximasColetas(LocalDate aPartirDe, int limite);

    /** Ids dos doadores ativos com doação mais recente, do mais recente para o mais antigo. */
    List<UUID> idsDoadoresRecentes(int limite);
}