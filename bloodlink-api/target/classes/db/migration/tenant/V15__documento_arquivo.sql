-- V15: o documento passa a apontar para um arquivo no armazenamento de objetos (S3 compatível).
-- A coluna "tipo" (V4) vira a categoria: EXAME, TERMO, TRIAGEM ou CERTIFICADO.

ALTER TABLE documento
    ALTER COLUMN url_arquivo DROP NOT NULL,
    ALTER COLUMN enviado_em TYPE TIMESTAMPTZ USING enviado_em AT TIME ZONE 'UTC',
    ADD COLUMN nome_arquivo        VARCHAR(150),
    ADD COLUMN tipo_conteudo       VARCHAR(40),
    ADD COLUMN tamanho_bytes       BIGINT,
    ADD COLUMN chave_armazenamento VARCHAR(300),
    ADD COLUMN enviado_por         UUID,
    ADD COLUMN enviado_por_papel   VARCHAR(20);

-- NOT VALID: vale para o que for gravado daqui para frente, sem reprovar linhas antigas
ALTER TABLE documento
    ADD CONSTRAINT ck_documento_categoria CHECK (tipo IN ('EXAME', 'TERMO', 'TRIAGEM', 'CERTIFICADO')) NOT VALID;

CREATE UNIQUE INDEX uk_documento_chave ON documento (chave_armazenamento) WHERE chave_armazenamento IS NOT NULL;
CREATE INDEX idx_documento_doador ON documento (doador_id, enviado_em DESC);
CREATE INDEX idx_documento_enviado_em ON documento (enviado_em DESC);
