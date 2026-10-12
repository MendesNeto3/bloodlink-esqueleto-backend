package com.bloodlink.api.shared.Pagina;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {
    public <R> Pagina<R> mapear(Function<T, R> funcao) {
        return new Pagina<>(conteudo.stream().map(funcao).toList(), pagina, tamanho, totalElementos, totalPaginas);
    }
}
