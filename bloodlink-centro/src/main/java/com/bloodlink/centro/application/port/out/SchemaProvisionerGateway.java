package com.bloodlink.centro.application.port.out;

import java.util.UUID;

public interface SchemaProvisionerGateway {
    void provisionarEmtodosOsServicos(UUID centroId, String schema, boolean ativo);
}
