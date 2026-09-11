CREATE TABLE estoque (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_sanguineo      VARCHAR(3) NOT NULL UNIQUE,
    quantidade_unidades INT NOT NULL DEFAULT 0,
    limiar_critico      INT NOT NULL DEFAULT 50
);

CREATE TABLE lote_estoque (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_sanguineo      VARCHAR(3) NOT NULL,
    quantidade_unidades INT NOT NULL,
    data_coleta         DATE NOT NULL,
    data_validade       DATE NOT NULL
);

CREATE INDEX idx_lote_tipo_validade ON lote_estoque (tipo_sanguineo, data_validade);
