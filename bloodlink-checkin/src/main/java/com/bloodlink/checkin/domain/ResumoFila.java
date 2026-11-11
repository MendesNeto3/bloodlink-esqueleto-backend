package com.bloodlink.checkin.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ResumoFila(LocalDate data, Map<SituacaoCheckin, Long> totais, List<ItemFila> itens) {
}