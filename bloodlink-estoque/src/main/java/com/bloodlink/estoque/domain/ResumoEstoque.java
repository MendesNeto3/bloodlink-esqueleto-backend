package com.bloodlink.estoque.domain;

import java.util.Comparator;
import java.util.List;

public record ResumoEstoque(Geral geral, List<String> tiposCriticos, List<PorTipo> porTipo) {

    public record Geral(long unidades, long meta, int percentual, int criticos, long vencendoEm7Dias) {
    }

    public record PorTipo(String tipoSanguineo, String rotulo, long unidades, int meta, int percentual,
                          int limiarCritico, boolean critico, long vencendoEm7Dias) {
    }

    public static ResumoEstoque de(List<SituacaoTipo> situacoes) {
        List<PorTipo> tipos = situacoes.stream()
                .sorted(Comparator.comparingInt(s -> s.tipo().ordinal()))
                .map(s -> new PorTipo(s.tipo().name(), s.tipo().rotulo(), s.unidades(), s.meta(),
                        percentual(s.unidades(), s.meta()), s.limiarCritico(),
                        s.unidades() <= s.limiarCritico(), s.vencendoEmBreve()))
                .toList();
        long unidades = tipos.stream().mapToLong(PorTipo::unidades).sum();
        long meta = tipos.stream().mapToLong(PorTipo::meta).sum();
        long vencendo = tipos.stream().mapToLong(PorTipo::vencendoEm7Dias).sum();
        List<String> criticos = tipos.stream().filter(PorTipo::critico).map(PorTipo::rotulo).toList();
        return new ResumoEstoque(
                new Geral(unidades, meta, percentual(unidades, meta), criticos.size(), vencendo),
                criticos, tipos);
    }

    private static int percentual(long unidades, long meta) {
        if (meta <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.round(unidades * 100.0 / meta));
    }
}