-- V9__agendamento.sql
CREATE TABLE agendamento (
                             id                   UUID         PRIMARY KEY,
                             doador_id            UUID         NOT NULL REFERENCES doador (id),
                             data                 DATE         NOT NULL,
                             hora                 TIME         NOT NULL,
                             status               VARCHAR(12)  NOT NULL,
                             origem               VARCHAR(10)  NOT NULL,
                             observacao           VARCHAR(500),
                             motivo_cancelamento  VARCHAR(300),
                             solicitado_por       UUID         NOT NULL,
                             doacao_id            UUID         REFERENCES doacao (id),
                             criado_em            TIMESTAMPTZ  NOT NULL,
                             atualizado_em        TIMESTAMPTZ  NOT NULL,
                             CONSTRAINT ck_agendamento_status CHECK (status IN ('PENDENTE', 'CONFIRMADO', 'CONCLUIDO', 'CANCELADO', 'FALTOU')),
                             CONSTRAINT ck_agendamento_origem CHECK (origem IN ('DOADOR', 'EQUIPE')),
                             CONSTRAINT ck_agendamento_conclusao CHECK (doacao_id IS NULL OR status = 'CONCLUIDO')
);

-- no máximo um agendamento ativo por doador, garantido pelo banco (vale mesmo com duas requisições ao mesmo tempo)
CREATE UNIQUE INDEX uk_agendamento_ativo_por_doador
    ON agendamento (doador_id) WHERE status IN ('PENDENTE', 'CONFIRMADO');

-- contagem de vagas por horário e agenda do dia
CREATE INDEX idx_agendamento_data_hora
    ON agendamento (data, hora) WHERE status IN ('PENDENTE', 'CONFIRMADO');

-- histórico do doador
CREATE INDEX idx_agendamento_doador ON agendamento (doador_id, data DESC);