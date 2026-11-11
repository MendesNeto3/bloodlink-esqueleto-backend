ALTER TABLE centro
    ADD COLUMN capacidade_diaria INT;

ALTER TABLE centro
    ADD CONSTRAINT ck_centro_capacidade CHECK (capacidade_diaria IS NULL OR capacidade_diaria > 0);