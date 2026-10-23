CREATE TABLE doacao (
                        id             UUID        PRIMARY KEY,
                        doador_id      UUID        NOT NULL REFERENCES doador (id),
                        data           DATE        NOT NULL,
                        volume_ml      INTEGER     NOT NULL,
                        registrada_por UUID        NOT NULL,
                        criada_em      TIMESTAMPTZ NOT NULL,
                        CONSTRAINT ck_doacao_volume CHECK (volume_ml BETWEEN 300 AND 600),
                        CONSTRAINT uk_doacao_doador_data UNIQUE (doador_id, data)
);
CREATE INDEX idx_doacao_doador_data ON doacao (doador_id, data DESC);

CREATE TABLE impedimento (
                             id             UUID         PRIMARY KEY,
                             doador_id      UUID         NOT NULL REFERENCES doador (id),
                             definitivo     BOOLEAN      NOT NULL,
                             inicio         DATE         NOT NULL,
                             fim            DATE,
                             motivo         VARCHAR(500) NOT NULL,
                             registrado_por UUID         NOT NULL,
                             criado_em      TIMESTAMPTZ  NOT NULL,
                             encerrado_em   TIMESTAMPTZ,
                             CONSTRAINT ck_impedimento_periodo CHECK (
                                 (definitivo AND fim IS NULL)
                                     OR (NOT definitivo AND fim IS NOT NULL AND fim >= inicio))
);
CREATE INDEX idx_impedimento_doador ON impedimento (doador_id) WHERE encerrado_em IS NULL;