CREATE TABLE tenant_provisionamento (
    id             UUID PRIMARY KEY,
    centro_id      UUID NOT NULL,
    "schema"       VARCHAR(63) NOT NULL UNIQUE,
    status         VARCHAR(30) NOT NULL,
    iniciado_em    TIMESTAMPTZ NOT NULL,
    atualizado_em  TIMESTAMPTZ NOT NULL,
    concluido_em   TIMESTAMPTZ
);