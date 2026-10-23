CREATE TABLE centro (
     id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     nome    VARCHAR(150) NOT NULL,
     schema  VARCHAR(63) NOT NULL UNIQUE,
     ativo   BOOLEAN NOT NULL DEFAULT true,
     CONSTRAINT ck_centro_schema CHECK (schema ~ '^centro_[a-f0-9]{12}$')
    );
