package com.bloodlink.api.doador.application.port.out;

import com.bloodlink.api.doador.domain.Doador;

import java.time.LocalDate;
import java.util.UUID;

public record ResumoDoadorResponse(UUID doadorId,
                                   String nome,
                                   String cidade,
                                   String uf,
                                   String tipoSanguineo,
                                   String centroId,
                                   Solicitacoes solicitacoes,
                                   Doacoes doacoes,
                                   Documentos documentos,
                                   ElegibilidadeResponse elegibilidade) {

    public record Solicitacoes(int ativas, ProximaSolicitacao proxima) {
    }

    /** Preenchido quando os agendamentos existirem. */
    public record ProximaSolicitacao(UUID id, LocalDate data, String hora, String status) {
    }

    public record Doacoes(int total, LocalDate ultima) {
    }

    public record Documentos(int total) {
    }

    public static ResumoDoadorResponse de(ResumoDoador resumo, String centroId) {
        Doador d = resumo.doador();
        return new ResumoDoadorResponse(
                d.id(),
                d.nome(),
                d.cidade(),
                d.uf(),
                d.tipoSanguineo() == null ? null : d.tipoSanguineo().name(),
                centroId,
                new Solicitacoes(0, null),      // até os agendamentos existirem
                new Doacoes(resumo.totalDoacoes(), resumo.ultimaDoacao()),
                new Documentos(0),              // até os documentos existirem
                ElegibilidadeResponse.de(resumo.elegibilidade()));
    }
}
