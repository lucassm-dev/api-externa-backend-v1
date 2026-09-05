ALTER TABLE operacao ADD COLUMN taxa_cambio_na_operacao NUMERIC(18,6) NOT NULL DEFAULT 1;
ALTER TABLE operacao ADD COLUMN data_hora_taxa_cambio TIMESTAMP;
ALTER TABLE operacao ADD COLUMN lucro_realizado_brl NUMERIC(18,4);

ALTER TABLE carteira_acao ADD COLUMN custo_total_brl NUMERIC(18,4) NOT NULL DEFAULT 0;