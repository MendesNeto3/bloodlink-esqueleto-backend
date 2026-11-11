package com.bloodlink.centro.adapter.in.web.dto;

import com.bloodlink.centro.domain.Centro;
import com.bloodlink.centro.domain.DiaSemana;
import com.bloodlink.centro.domain.PerfilCentro;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Visão da plataforma: tudo que o card de "Centros cadastrados" mostra. */
public record CentroDetalheResponse(UUID id,
                                    String centroId,
                                    String codigo,
                                    String nome,
                                    boolean ativo,
                                    String tipo,
                                    String endereco,
                                    String cidade,
                                    String uf,
                                    BigDecimal latitude,
                                    BigDecimal longitude,
                                    List<String> diasFuncionamento,
                                    String abertura,
                                    String fechamento,
                                    Integer capacidadeDiaria) {

    public static CentroDetalheResponse de(Centro centro) {
        PerfilCentro p = centro.perfil();
        String centroId = centro.centroId();
        return new CentroDetalheResponse(
                centro.id(),
                centroId,
                "CTR-" + centroId.substring(0, 6).toUpperCase(),
                centro.nome(),
                centro.ativo(),
                p.tipo().name(),
                p.endereco(),
                p.cidade(),
                p.uf(),
                p.latitude(),
                p.longitude(),
                p.diasFuncionamento().stream().map(DiaSemana::name).toList(),
                p.abertura() == null ? null : p.abertura().toString(),
                p.fechamento() == null ? null : p.fechamento().toString(),
                p.capacidadeDiaria());
    }
}