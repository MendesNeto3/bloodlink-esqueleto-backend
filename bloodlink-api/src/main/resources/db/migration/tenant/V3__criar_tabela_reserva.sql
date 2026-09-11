CREATE TABLE reserva (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_nome       VARCHAR(150) NOT NULL,
    tipo_sanguineo      VARCHAR(3) NOT NULL,
    quantidade_unidades INT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    solicitado_em       TIMESTAMP NOT NULL DEFAULT now(),
    tipo_atendido_com   VARCHAR(3) -- pode ser diferente de tipo_sanguineo quando atendida com um tipo compatível
);
