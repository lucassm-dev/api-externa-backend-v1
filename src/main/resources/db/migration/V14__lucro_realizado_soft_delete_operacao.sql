ALTER TABLE operacao ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE operacao ADD COLUMN preco_medio_compra_no_momento NUMERIC(18,4);
ALTER TABLE operacao ADD COLUMN lucro_realizado NUMERIC(18,4);
