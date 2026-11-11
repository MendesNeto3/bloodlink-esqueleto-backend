CREATE TABLE estoque (
                         tipo_sanguineo VARCHAR(12) PRIMARY KEY,
                         limiar_critico INT NOT NULL DEFAULT 50,
                         meta_unidades  INT NOT NULL DEFAULT 500,
                         CONSTRAINT ck_estoque_tipo CHECK (tipo_sanguineo IN ('A_POSITIVO','A_NEGATIVO','B_POSITIVO','B_NEGATIVO',
                                                                              'AB_POSITIVO','AB_NEGATIVO','O_POSITIVO','O_NEGATIVO')),
                         CONSTRAINT ck_estoque_limiar CHECK (limiar_critico >= 0),
                         CONSTRAINT ck_estoque_meta CHECK (meta_unidades > 0)
);

INSERT INTO estoque (tipo_sanguineo) VALUES
                                         ('A_POSITIVO'), ('A_NEGATIVO'), ('B_POSITIVO'), ('B_NEGATIVO'),
                                         ('AB_POSITIVO'), ('AB_NEGATIVO'), ('O_POSITIVO'), ('O_NEGATIVO');

CREATE TABLE lote_estoque (
                              id                  UUID PRIMARY KEY,
                              tipo_sanguineo      VARCHAR(12) NOT NULL REFERENCES estoque (tipo_sanguineo),
                              quantidade_unidades INT NOT NULL,
                              data_coleta         DATE NOT NULL,
                              data_validade       DATE NOT NULL,
                              registrado_por      UUID NOT NULL,
                              criado_em           TIMESTAMPTZ NOT NULL DEFAULT now(),
                              CONSTRAINT ck_lote_quantidade CHECK (quantidade_unidades >= 0),
                              CONSTRAINT ck_lote_validade CHECK (data_validade >= data_coleta)
);

CREATE INDEX idx_lote_tipo_validade ON lote_estoque (tipo_sanguineo, data_validade);