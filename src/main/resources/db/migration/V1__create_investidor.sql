CREATE TABLE investidor (
    id    BIGSERIAL PRIMARY KEY,
    nome  VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    cpf   VARCHAR(14)  NOT NULL,
    CONSTRAINT uk_investidor_email UNIQUE (email),
    CONSTRAINT uk_investidor_cpf   UNIQUE (cpf)
);
