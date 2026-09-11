CREATE TABLE checkin (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agendamento_id  UUID NOT NULL,
    horario_chegada TIMESTAMP NOT NULL DEFAULT now(),
    triagem_ok      BOOLEAN NOT NULL DEFAULT false
);
