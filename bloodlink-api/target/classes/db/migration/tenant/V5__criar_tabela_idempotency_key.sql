CREATE TABLE idempotency_key (
    chave           VARCHAR(100) PRIMARY KEY,
    agendamento_id  UUID NOT NULL,
    criado_em       TIMESTAMP NOT NULL DEFAULT now()
);
