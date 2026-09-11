CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE doador (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(150) NOT NULL,
    cpf             VARCHAR(11) NOT NULL UNIQUE,
    email           VARCHAR(150) NOT NULL,
    tipo_sanguineo  VARCHAR(3) NOT NULL,
    sexo            VARCHAR(1) NOT NULL, -- 'M' ou 'F' — usado no intervalo mínimo entre doações
    peso_kg         NUMERIC(5,2) NOT NULL,
    nascimento      DATE NOT NULL,
    ultima_doacao   DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ELEGIVEL'
);
