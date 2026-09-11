CREATE TABLE documento (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo         VARCHAR(50) NOT NULL,
    doador_id    UUID REFERENCES doador (id),
    url_arquivo  TEXT NOT NULL,
    enviado_em   TIMESTAMP NOT NULL DEFAULT now()
);
