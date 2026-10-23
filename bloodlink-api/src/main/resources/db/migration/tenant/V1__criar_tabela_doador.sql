
CREATE TABLE doador (
                        id                        UUID PRIMARY KEY,
                        nome                      VARCHAR(120) NOT NULL,
                        cpf                       VARCHAR(11)  NOT NULL,
                        data_nascimento           DATE         NOT NULL,
                        sexo                      VARCHAR(10)  NOT NULL,
                        tipo_sanguineo            VARCHAR(12)  NOT NULL,
                        peso_kg                   NUMERIC(5,2) NOT NULL,
                        telefone                  VARCHAR(11),
                        email                     VARCHAR(120),
                        consentimento_responsavel BOOLEAN      NOT NULL DEFAULT FALSE,
                        ativo                     BOOLEAN      NOT NULL DEFAULT TRUE,
                        criado_em                 TIMESTAMPTZ  NOT NULL,
                        atualizado_em             TIMESTAMPTZ  NOT NULL,
                        CONSTRAINT uk_doador_cpf UNIQUE (cpf),
                        CONSTRAINT ck_doador_sexo CHECK (sexo IN ('MASCULINO', 'FEMININO')),
                        CONSTRAINT ck_doador_tipo CHECK (tipo_sanguineo IN ('A_POSITIVO','A_NEGATIVO','B_POSITIVO','B_NEGATIVO',
                                                                            'AB_POSITIVO','AB_NEGATIVO','O_POSITIVO','O_NEGATIVO')),
                        CONSTRAINT ck_doador_peso CHECK (peso_kg > 0)
);
CREATE INDEX idx_doador_nome ON doador (lower(nome));