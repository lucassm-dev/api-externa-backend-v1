-- #28: código de troca de e-mail guarda o endereço novo; nulo = recuperação de senha
ALTER TABLE codigo_recuperacao ADD COLUMN novo_email VARCHAR(255);
