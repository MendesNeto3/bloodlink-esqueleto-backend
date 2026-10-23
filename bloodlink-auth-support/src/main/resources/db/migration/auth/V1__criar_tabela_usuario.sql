CREATE TABLE usuario (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(120) NOT NULL,
    nome          VARCHAR(120) NOT NULL,
    senha_hash    VARCHAR(100) NOT NULL,
    papel         VARCHAR(20)  NOT NULL,
    centro_id     VARCHAR(36),
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMPTZ  NOT NULL,
    atualizado_em TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_email_minusculo CHECK (email = lower(email)),
    CONSTRAINT ck_usuario_papel CHECK (papel IN ('ADMIN_PLATAFORMA', 'ADMIN_CENTRO', 'RECEPCAO')),
    -- o administrador da plataforma não pertence a centro; os demais papéis exigem um
    CONSTRAINT ck_usuario_centro CHECK (
    (papel = 'ADMIN_PLATAFORMA' AND centro_id IS NULL)
    OR (papel <> 'ADMIN_PLATAFORMA' AND centro_id IS NOT NULL))
);

CREATE INDEX idx_usuario_centro ON usuario (centro_id);
CREATE INDEX idx_usuario_nome ON usuario (lower(nome));