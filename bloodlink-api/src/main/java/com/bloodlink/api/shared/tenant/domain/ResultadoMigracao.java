package com.bloodlink.api.shared.tenant.domain;

import java.util.Map;

public record ResultadoMigracao(Map<String, Integer> migrados, Map<String, String> falhas) {
    public int total(){
        return migrados.size() + falhas.size();
    }
}
