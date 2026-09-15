package com.bloodlink.centro.domain;

import java.util.UUID;

public record Centro  (
        UUID id,
        String nome,
        String schema,
        boolean ativo
){
}
