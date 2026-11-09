package com.bloodlink.estoque.domain;

public record SituacaoTipo(TipoSanguineo tipo, long unidades, int meta, int limiarCritico, long vencendoEmBreve) {
}