package com.bloodlink.tenant;

public interface SchemaProvisionerPort {
    void provisionar (String schema);
    int migrar(String schema);
}
