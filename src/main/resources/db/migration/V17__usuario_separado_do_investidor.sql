-- ADR-012 / spec-15: acesso (e-mail, senha, papel, situação) sai de investidor para usuario.
-- investidor.id passa a ser o próprio usuario.id, então os ids migrados são preservados.
CREATE TABLE usuario (
    id       BIGSERIAL    PRIMARY KEY,
    email    VARCHAR(255) NOT NULL,
    senha    VARCHAR(255) NOT NULL,
    papel    VARCHAR(20)  NOT NULL,
    situacao VARCHAR(20)  NOT NULL
);

INSERT INTO usuario (id, email, senha, papel, situacao)
SELECT id, email, senha, 'INVESTIDOR', CASE WHEN ativo THEN 'ATIVO' ELSE 'EXCLUIDO' END
FROM investidor;

SELECT setval(pg_get_serial_sequence('usuario', 'id'), COALESCE((SELECT MAX(id) FROM usuario), 0) + 1, false);

CREATE UNIQUE INDEX uk_usuario_email_nao_excluido ON usuario (email) WHERE situacao <> 'EXCLUIDO';

DROP INDEX uk_investidor_email_ativo;
ALTER TABLE investidor DROP COLUMN email;
ALTER TABLE investidor DROP COLUMN senha;
ALTER TABLE investidor ALTER COLUMN id DROP DEFAULT;
ALTER TABLE investidor ADD CONSTRAINT fk_investidor_usuario FOREIGN KEY (id) REFERENCES usuario (id);
