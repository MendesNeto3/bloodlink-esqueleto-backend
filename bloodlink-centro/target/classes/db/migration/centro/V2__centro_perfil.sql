
ALTER TABLE centro
    ADD COLUMN tipo                VARCHAR(10) NOT NULL DEFAULT 'FIXO',
    ADD COLUMN endereco            VARCHAR(200),
    ADD COLUMN cidade              VARCHAR(100),
    ADD COLUMN uf                  VARCHAR(2),
    ADD COLUMN latitude            NUMERIC(9,6),
    ADD COLUMN longitude           NUMERIC(9,6),
    ADD COLUMN dias_funcionamento  VARCHAR(27),
    ADD COLUMN abertura            TIME,
    ADD COLUMN fechamento          TIME;

ALTER TABLE centro
    ADD CONSTRAINT ck_centro_tipo CHECK (tipo IN ('FIXO', 'POSTO', 'MOVEL')),
    ADD CONSTRAINT ck_centro_uf CHECK (uf IS NULL OR uf ~ '^[A-Z]{2}$'),
    ADD CONSTRAINT ck_centro_coordenadas CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)),
    ADD CONSTRAINT ck_centro_horario CHECK (
        (abertura IS NULL AND fechamento IS NULL)
        OR (abertura IS NOT NULL AND fechamento IS NOT NULL AND abertura < fechamento));