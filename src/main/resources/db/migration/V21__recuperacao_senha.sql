-- #26: tokens emitidos antes desta data deixam de valer
ALTER TABLE usuario ADD COLUMN senha_alterada_em TIMESTAMP WITH TIME ZONE;

CREATE TABLE codigo_recuperacao (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuario (id),
    codigo_hash VARCHAR(100) NOT NULL,
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    tentativas  INT NOT NULL DEFAULT 0,
    usado_em    TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_codigo_recuperacao_usuario ON codigo_recuperacao (usuario_id, criado_em);
