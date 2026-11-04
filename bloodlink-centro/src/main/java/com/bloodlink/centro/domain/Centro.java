package com.bloodlink.centro.domain;

import java.util.UUID;

public record Centro(UUID id, String nome, String schema, boolean ativo, PerfilCentro perfil) {

    private static final String PREFIXO_SCHEMA = "centro_";

    public Centro {
        perfil = perfil == null ? PerfilCentro.vazio() : perfil;
    }

    /** Centro recém-registrado: perfil vazio. */
    public Centro(UUID id, String nome, String schema, boolean ativo) {
        this(id, nome, schema, ativo, PerfilCentro.vazio());
    }

    /** Os 12 hexadecimais do schema, o mesmo valor do centroId do token. */
    public String centroId() {
        return schema.substring(PREFIXO_SCHEMA.length());
    }

    public Centro comNome(String novoNome) {
        return new Centro(id, novoNome, schema, ativo, perfil);
    }

    public Centro desativado() {
        return new Centro(id, nome, schema, false, perfil);
    }

    public Centro comPerfil(PerfilCentro novoPerfil) {
        return new Centro(id, nome, schema, ativo, novoPerfil);
    }
}