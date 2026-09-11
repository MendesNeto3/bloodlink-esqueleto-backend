CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE centro (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome    VARCHAR(150) NOT NULL,
    schema  VARCHAR(63) NOT NULL UNIQUE,
    ativo   BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE usuario (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(150) NOT NULL UNIQUE,
    senha_hash  VARCHAR(60) NOT NULL,  -- tamanho fixo do BCrypt
    papel       VARCHAR(20) NOT NULL,
    centro_id   UUID NOT NULL REFERENCES centro (id)
);
