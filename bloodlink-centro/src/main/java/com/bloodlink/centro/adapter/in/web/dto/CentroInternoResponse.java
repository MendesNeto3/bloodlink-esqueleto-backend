package com.bloodlink.centro.adapter.in.web.dto;

/** O mínimo que os outros serviços precisam saber: sem schema, sem id interno, sem perfil. */
public record CentroInternoResponse(String centroId, String nome, boolean ativo) {
}