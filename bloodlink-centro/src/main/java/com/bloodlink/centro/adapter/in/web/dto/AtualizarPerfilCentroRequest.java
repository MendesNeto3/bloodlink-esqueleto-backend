package com.bloodlink.centro.adapter.in.web.dto;

import com.bloodlink.centro.domain.DiaSemana;
import com.bloodlink.centro.domain.PerfilCentro;
import com.bloodlink.centro.domain.TipoCentro;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

public record AtualizarPerfilCentroRequest(TipoCentro tipo,
                                           String endereco,
                                           String cidade,
                                           String uf,
                                           BigDecimal latitude,
                                           BigDecimal longitude,
                                           List<DiaSemana> diasFuncionamento,
                                           LocalTime abertura,
                                           LocalTime fechamento,
                                           Integer capacidadeDiaria) {

    public PerfilCentro paraPerfil() {
        return new PerfilCentro(tipo, endereco, cidade, uf, latitude, longitude,
                diasFuncionamento == null || diasFuncionamento.isEmpty()
                        ? null : EnumSet.copyOf(diasFuncionamento),
                abertura, fechamento, capacidadeDiaria);
    }
}