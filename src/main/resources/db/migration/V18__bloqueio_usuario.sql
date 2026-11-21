-- spec-18 (#21): motivo do bloqueio fica no próprio usuário; o histórico vem com a #22
ALTER TABLE usuario ADD COLUMN motivo_bloqueio VARCHAR(255);
ALTER TABLE usuario ADD COLUMN bloqueado_em TIMESTAMP;
