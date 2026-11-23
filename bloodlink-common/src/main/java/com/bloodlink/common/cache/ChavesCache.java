package com.bloodlink.common.cache;

/** Chaves de cache usadas por mais de um serviço. As de uso local ficam no próprio módulo. */
public final class ChavesCache {

    private static final String PREFIXO = "bloodlink:";

    private ChavesCache() { }

    /** Nome do hemocentro, guardado pela API e apagado pelo Centro quando o nome muda. centroId = 12 hex. */
    public static String nomeCentro(String centroId) {
        return PREFIXO + "centro:nome:" + centroId;
    }
}
