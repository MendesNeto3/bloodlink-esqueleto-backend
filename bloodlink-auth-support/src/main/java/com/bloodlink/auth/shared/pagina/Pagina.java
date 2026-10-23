package com.bloodlink.auth.shared.pagina;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {

    public <R> Pagina<R> mapear(Function<? super T, ? extends R> conversor) {
        return new Pagina<>(conteudo.stream().<R>map(conversor).toList(), pagina, tamanho, totalElementos, totalPaginas);
    }
}
