package com.bloodlink.centro.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record PerfilCentro(TipoCentro tipo,
                           String endereco,
                           String cidade,
                           String uf,
                           BigDecimal latitude,
                           BigDecimal longitude,
                           Set<DiaSemana> diasFuncionamento,
                           LocalTime abertura,
                           LocalTime fechamento) {

    private static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    public PerfilCentro {
        tipo = tipo == null ? TipoCentro.FIXO : tipo;
        endereco = texto(endereco, 200, "O endereço");
        cidade = texto(cidade, 100, "A cidade");
        uf = uf(uf);
        if ((cidade == null) != (uf == null)) {
            throw new IllegalArgumentException("Informe cidade e UF juntas");
        }
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Informe latitude e longitude juntas");
        }
        if (latitude != null) {
            if (latitude.doubleValue() < -90 || latitude.doubleValue() > 90
                    || longitude.doubleValue() < -180 || longitude.doubleValue() > 180) {
                throw new IllegalArgumentException("Coordenadas fora do intervalo válido");
            }
            latitude = latitude.setScale(6, RoundingMode.HALF_UP);
            longitude = longitude.setScale(6, RoundingMode.HALF_UP);
        }
        if ((abertura == null) != (fechamento == null)) {
            throw new IllegalArgumentException("Informe abertura e fechamento juntos");
        }
        if (abertura != null && !abertura.isBefore(fechamento)) {
            throw new IllegalArgumentException("A abertura deve ser antes do fechamento");
        }
        diasFuncionamento = (diasFuncionamento == null || diasFuncionamento.isEmpty())
                ? Collections.unmodifiableSet(EnumSet.noneOf(DiaSemana.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(diasFuncionamento));
    }

    public static PerfilCentro vazio() {
        return new PerfilCentro(null, null, null, null, null, null, null, null, null);
    }

    private static String texto(String valor, int max, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.trim();
        if (limpo.length() > max) {
            throw new IllegalArgumentException(campo + " excede o tamanho máximo de " + max + " caracteres");
        }
        return limpo;
    }

    private static String uf(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.trim().toUpperCase();
        if (!UFS.contains(limpo)) {
            throw new IllegalArgumentException("UF inválida");
        }
        return limpo;
    }
}