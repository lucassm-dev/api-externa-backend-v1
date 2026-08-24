CREATE TABLE acao (
    id                BIGSERIAL    PRIMARY KEY,
    ticker            VARCHAR(20)  NOT NULL,
    nome_empresa      VARCHAR(255),
    mercado           VARCHAR(2)   NOT NULL,
    moeda             VARCHAR(3)   NOT NULL,
    cotacao_atual     NUMERIC(18,4),
    data_hora_cotacao TIMESTAMP,
    CONSTRAINT uk_acao_ticker UNIQUE (ticker)
);
