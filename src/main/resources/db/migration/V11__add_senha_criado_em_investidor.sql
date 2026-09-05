ALTER TABLE investidor ADD COLUMN senha VARCHAR(255);
ALTER TABLE investidor ADD COLUMN criado_em TIMESTAMP NOT NULL DEFAULT now();

-- Investidores cadastrados antes da SPEC-02 (sem senha, ex.: dados de teste do Bloco A)
-- recebem um hash inválido — a conta continua existindo (preserva FK de carteira/operação),
-- mas ninguém consegue logar nela; um cadastro novo com o mesmo e-mail seguiria bloqueado
-- por unicidade, então essas contas legadas precisam ser recriadas via /auth/cadastro
-- com outro e-mail se o dono precisar logar.
UPDATE investidor SET senha = '$2a$10$invalidoinvalido12345.invalidoinvalidoinvalidoinvalid' WHERE senha IS NULL;

ALTER TABLE investidor ALTER COLUMN senha SET NOT NULL;