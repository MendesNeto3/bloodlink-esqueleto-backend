CREATE TABLE refresh_token (
    id         UUID        PRIMARY KEY,
    usuario_id UUID        NOT NULL REFERENCES usuario (id),
    token_hash VARCHAR(64) NOT NULL,
    expira_em  TIMESTAMPTZ NOT NULL,
    revogado   BOOLEAN     NOT NULL DEFAULT FALSE,
    criado_em  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_usuario ON refresh_token (usuario_id);
CREATE INDEX idx_refresh_token_expira ON refresh_token (expira_em);