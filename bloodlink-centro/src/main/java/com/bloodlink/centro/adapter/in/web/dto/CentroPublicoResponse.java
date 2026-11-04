package com.bloodlink.centro.adapter.in.web.dto;

import com.bloodlink.centro.domain.Centro;
import com.bloodlink.centro.domain.CentroComDistancia;
import com.bloodlink.centro.domain.DiaSemana;
import com.bloodlink.centro.domain.PerfilCentro;

import java.math.BigDecimal;
import java.util.List;

public record CentroPublicoResponse(String centroId,
                                    String nome,
                                    String tipo,
                                    String endereco,
                                    String cidade,
                                    String uf,
                                    BigDecimal latitude,
                                    BigDecimal longitude,
                                    List<String> diasFuncionamento,
                                    String abertura,
                                    String fechamento,
                                    Double distanciaKm) {

    public static CentroPublicoResponse de(CentroComDistancia item) {
        return de(item.centro(), item.distanciaKm());
    }

    public static CentroPublicoResponse de(Centro centro, Double distanciaKm) {
        PerfilCentro p = centro.perfil();
        return new CentroPublicoResponse(
                centro.centroId(),
                centro.nome(),
                p.tipo().name(),
                p.endereco(),
                p.cidade(),
                p.uf(),
                p.latitude(),
                p.longitude(),
                p.diasFuncionamento().stream().map(DiaSemana::name).toList(),
                p.abertura() == null ? null : p.abertura().toString(),
                p.fechamento() == null ? null : p.fechamento().toString(),
                distanciaKm == null ? null : Math.round(distanciaKm * 10.0) / 10.0);
    }
}
