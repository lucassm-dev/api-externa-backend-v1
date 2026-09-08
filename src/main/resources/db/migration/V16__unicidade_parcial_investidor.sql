ALTER TABLE investidor DROP CONSTRAINT uk_investidor_email;
CREATE UNIQUE INDEX uk_investidor_email_ativo ON investidor (email) WHERE ativo = true;

ALTER TABLE investidor DROP CONSTRAINT uk_investidor_cpf;
CREATE UNIQUE INDEX uk_investidor_cpf_ativo ON investidor (cpf) WHERE ativo = true;
