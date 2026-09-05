ALTER TABLE acao DROP CONSTRAINT uk_acao_ticker;
CREATE UNIQUE INDEX uk_acao_ticker_ativo ON acao (ticker) WHERE ativo = true;

ALTER TABLE corretora DROP CONSTRAINT uk_corretora_cnpj;
CREATE UNIQUE INDEX uk_corretora_cnpj_ativo ON corretora (cnpj) WHERE ativo = true;