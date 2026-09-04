ALTER TABLE investidor ADD COLUMN senha VARCHAR(255);
ALTER TABLE investidor ADD COLUMN criado_em TIMESTAMP NOT NULL DEFAULT now();

-- Sem dado existente com senha ainda (MVP sem usuários reais em produção) — sem backfill necessário.
ALTER TABLE investidor ALTER COLUMN senha SET NOT NULL;