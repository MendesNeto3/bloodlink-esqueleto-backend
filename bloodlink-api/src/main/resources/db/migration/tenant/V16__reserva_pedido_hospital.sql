-- V16: a reserva passa a ser o pedido de um hospital (tipo, unidades e validade).
-- Ela só REGISTRA o pedido: não mexe no estoque. "Vencida" não é gravada: vem da data de validade.

ALTER TABLE reserva
    ALTER COLUMN solicitado_em TYPE TIMESTAMPTZ USING solicitado_em AT TIME ZONE 'UTC',
    ADD COLUMN valida_ate  DATE,
    ADD COLUMN criado_por  UUID,
    ADD COLUMN liberada_em TIMESTAMPTZ;

-- linhas do modelo antigo (se houver): sete dias de validade a partir do pedido
UPDATE reserva SET valida_ate = solicitado_em::date + 7 WHERE valida_ate IS NULL;
UPDATE reserva SET status = 'ATIVA' WHERE status = 'PENDENTE';

ALTER TABLE reserva
    ALTER COLUMN valida_ate SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'ATIVA';

-- NOT VALID: vale para o que for gravado daqui para frente, sem reprovar linhas antigas
ALTER TABLE reserva ADD CONSTRAINT ck_reserva_status   CHECK (status IN ('ATIVA', 'LIBERADA')) NOT VALID;
ALTER TABLE reserva ADD CONSTRAINT ck_reserva_unidades CHECK (quantidade_unidades > 0) NOT VALID;

CREATE INDEX idx_reserva_solicitado_em ON reserva (solicitado_em DESC);
