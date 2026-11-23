CREATE TABLE movimentacao_estoque (
                                      id                 UUID PRIMARY KEY,
                                      tipo_sanguineo     VARCHAR(12) NOT NULL REFERENCES estoque (tipo_sanguineo),
                                      lote_id            UUID NOT NULL,
                                      tipo_movimentacao  VARCHAR(10) NOT NULL,
                                      quantidade         INT NOT NULL,
                                      motivo             VARCHAR(20),
                                      registrado_por     UUID NOT NULL,
                                      chave_idempotencia VARCHAR(80),
                                      criado_em          TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      CONSTRAINT ck_mov_tipo CHECK (tipo_movimentacao IN ('ENTRADA', 'BAIXA')),
                                      CONSTRAINT ck_mov_quantidade CHECK (quantidade > 0),
                                      CONSTRAINT ck_mov_motivo CHECK (motivo IS NULL OR motivo IN ('TRANSFUSAO', 'DESCARTE', 'OUTRO')),
                                      CONSTRAINT ck_mov_motivo_baixa CHECK ((tipo_movimentacao = 'BAIXA') = (motivo IS NOT NULL)),
                                      CONSTRAINT ck_mov_chave_entrada CHECK (chave_idempotencia IS NULL OR tipo_movimentacao = 'ENTRADA'),
                                      CONSTRAINT uq_mov_chave UNIQUE (chave_idempotencia)
);

-- lote_id não tem chave estrangeira de propósito: o lote é apagado quando zera e o histórico fica
CREATE INDEX idx_mov_tipo_criado ON movimentacao_estoque (tipo_sanguineo, criado_em DESC);
CREATE INDEX idx_mov_criado ON movimentacao_estoque (criado_em DESC);

DELETE FROM lote_estoque WHERE quantidade_unidades = 0;