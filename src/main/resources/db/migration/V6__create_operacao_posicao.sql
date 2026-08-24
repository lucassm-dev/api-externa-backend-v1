CREATE TABLE operacao (
    id              BIGSERIAL     PRIMARY KEY,
    carteira_id     BIGINT        NOT NULL REFERENCES carteira(id),
    acao_id         BIGINT        NOT NULL REFERENCES acao(id),
    tipo            VARCHAR(6)    NOT NULL CHECK (tipo IN ('COMPRA', 'VENDA')),
    quantidade      INT           NOT NULL CHECK (quantidade > 0),
    preco_unitario  NUMERIC(18,4) NOT NULL,
    data_hora       TIMESTAMP     NOT NULL
);

CREATE INDEX idx_operacao_carteira ON operacao (carteira_id);
CREATE INDEX idx_operacao_investidor ON operacao (carteira_id, acao_id, data_hora);

CREATE TABLE carteira_acao (
    id           BIGSERIAL     PRIMARY KEY,
    carteira_id  BIGINT        NOT NULL REFERENCES carteira(id),
    acao_id      BIGINT        NOT NULL REFERENCES acao(id),
    quantidade   INT           NOT NULL,
    preco_medio  NUMERIC(18,4) NOT NULL,
    CONSTRAINT uk_carteira_acao UNIQUE (carteira_id, acao_id)
);
