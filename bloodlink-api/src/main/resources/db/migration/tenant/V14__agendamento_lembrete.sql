-- Marca o agendamento que já recebeu o lembrete do dia. Nulo = lembrete ainda não enviado.
ALTER TABLE agendamento
    ADD COLUMN lembrete_enviado_em TIMESTAMPTZ;

-- O job do lembrete só olha confirmados de um dia que ainda não receberam o aviso.
CREATE INDEX idx_agendamento_lembrete_pendente
    ON agendamento (data)
    WHERE status = 'CONFIRMADO' AND lembrete_enviado_em IS NULL;
