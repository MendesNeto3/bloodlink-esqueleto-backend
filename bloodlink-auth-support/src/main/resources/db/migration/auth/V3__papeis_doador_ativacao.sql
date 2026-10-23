-- Novos papéis (ENFERMEIRO, DOADOR), status de ativação, vínculo com o doador e tokens de ativação.

ALTER TABLE usuario DROP CONSTRAINT ck_usuario_papel;
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_papel CHECK (
    papel IN ('ADMIN_PLATAFORMA', 'ADMIN_CENTRO', 'RECEPCAO', 'ENFERMEIRO', 'DOADOR'));

ALTER TABLE usuario ADD COLUMN doador_id UUID;
ALTER TABLE usuario ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';

-- conta pendente ainda não tem senha
ALTER TABLE usuario ALTER COLUMN senha_hash DROP NOT NULL;

ALTER TABLE usuario ADD CONSTRAINT ck_usuario_status CHECK (status IN ('PENDENTE', 'ATIVO'));
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_senha CHECK (status = 'PENDENTE' OR senha_hash IS NOT NULL);
-- DOADOR exige o vínculo com o cadastro do doador; os demais papéis não podem tê-lo
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_doador CHECK (
    (papel = 'DOADOR' AND doador_id IS NOT NULL)
        OR (papel <> 'DOADOR' AND doador_id IS NULL));

-- uma conta por doador em cada centro
CREATE UNIQUE INDEX uk_usuario_centro_doador ON usuario (centro_id, doador_id) WHERE doador_id IS NOT NULL;

CREATE TABLE token_ativacao (
                                id          UUID         PRIMARY KEY,
                                usuario_id  UUID         NOT NULL,
                                token_hash  VARCHAR(64)  NOT NULL,
                                expira_em   TIMESTAMPTZ  NOT NULL,
                                usado_em    TIMESTAMPTZ,
                                criado_em   TIMESTAMPTZ  NOT NULL,
                                CONSTRAINT fk_token_ativacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
                                CONSTRAINT uk_token_ativacao_hash UNIQUE (token_hash)
);

CREATE INDEX idx_token_ativacao_usuario ON token_ativacao (usuario_id);