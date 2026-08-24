CREATE TABLE carteira (
    id             BIGSERIAL   PRIMARY KEY,
    investidor_id  BIGINT      NOT NULL REFERENCES investidor(id),
    corretora_id   BIGINT      NOT NULL REFERENCES corretora(id),
    mercado        VARCHAR(2)  NOT NULL,
    nome           VARCHAR(255) NOT NULL,
    ativa          BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_carteira_investidor ON carteira (investidor_id);
