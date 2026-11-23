CREATE TABLE checkin (
                         id                  UUID         PRIMARY KEY,
                         agendamento_id      UUID         NOT NULL REFERENCES agendamento (id),
                         doador_id           UUID         NOT NULL REFERENCES doador (id),
                         chegada_em          TIMESTAMPTZ  NOT NULL,
                         registrado_por      UUID         NOT NULL,
                         triagem_resultado   VARCHAR(10),
                         triagem_observacao  VARCHAR(500),
                         triagem_em          TIMESTAMPTZ,
                         triagem_por         UUID,
                         CONSTRAINT uk_checkin_agendamento UNIQUE (agendamento_id),
                         CONSTRAINT ck_checkin_resultado CHECK (triagem_resultado IS NULL OR triagem_resultado IN ('APTA', 'INAPTA')),
                         CONSTRAINT ck_checkin_triagem_completa CHECK (
                             (triagem_resultado IS NULL AND triagem_em IS NULL AND triagem_por IS NULL)
                                 OR (triagem_resultado IS NOT NULL AND triagem_em IS NOT NULL AND triagem_por IS NOT NULL)),
                         CONSTRAINT ck_checkin_inapta_motivo CHECK (triagem_resultado IS DISTINCT FROM 'INAPTA' OR triagem_observacao IS NOT NULL)
    );

CREATE INDEX idx_checkin_chegada ON checkin (chegada_em);