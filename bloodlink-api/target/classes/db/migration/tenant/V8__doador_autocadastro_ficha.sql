ALTER TABLE doador
    ADD COLUMN cidade                  VARCHAR(80),
    ADD COLUMN uf                      VARCHAR(2),
    ADD COLUMN ultima_doacao_declarada DATE,
    ADD COLUMN origem_cadastro         VARCHAR(20) NOT NULL DEFAULT 'EQUIPE',
    ADD COLUMN consentimento_lgpd_em   TIMESTAMPTZ,
    ADD CONSTRAINT ck_doador_origem CHECK (origem_cadastro IN ('EQUIPE', 'AUTOCADASTRO')),
    ADD CONSTRAINT ck_doador_uf CHECK (uf IS NULL OR uf ~ '^[A-Z]{2}$');

-- Dado de saúde sensível (LGPD): nunca vai para log.
CREATE TABLE ficha_pre_doacao (
       doador_id             UUID         PRIMARY KEY REFERENCES doador (id) ON DELETE CASCADE,
       bem_de_saude          BOOLEAN      NOT NULL,
       dormiu_6h             BOOLEAN      NOT NULL,
       alimentado_hidratado  BOOLEAN      NOT NULL,
       alcool_12h            BOOLEAN      NOT NULL,
       procedimento_recente  BOOLEAN      NOT NULL,
       gestante_amamentando  BOOLEAN      NOT NULL,
       doenca_relevante      BOOLEAN      NOT NULL,
       medicamento_ou_vacina BOOLEAN      NOT NULL,
       observacoes           VARCHAR(1000),
       atualizada_em         TIMESTAMPTZ  NOT NULL
);