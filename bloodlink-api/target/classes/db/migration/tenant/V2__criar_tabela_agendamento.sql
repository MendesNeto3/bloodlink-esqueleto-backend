CREATE TABLE agendamento (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doador_id     UUID NOT NULL,
    doador_nome   VARCHAR(150) NOT NULL,
    doador_email  VARCHAR(150) NOT NULL,
    data_hora     TIMESTAMP NOT NULL,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_agendamento_data_status ON agendamento (data_hora, status);
