package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.domain.Doacao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DoacaoRepositoryPort {

    Doacao salvar(Doacao doacao);

    boolean existePorDoadorEData(UUID doadorId, LocalDate data);

    /** Histórico do doador, da mais recente para a mais antiga. */
    List<Doacao> listarPorDoador(UUID doadorId);

    /** Datas das doações de vários doadores em uma consulta só (evita uma consulta por doador na listagem). */
    Map<UUID, List<LocalDate>> datasPorDoadores(Collection<UUID> doadorIds);
}
