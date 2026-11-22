-- ADR-017: foto de perfil no banco, fora de usuario para não pesar nas consultas de autenticação
CREATE TABLE foto_perfil (
    usuario_id    BIGINT PRIMARY KEY REFERENCES usuario (id),
    conteudo      BYTEA NOT NULL,
    tipo          VARCHAR(20) NOT NULL,
    atualizada_em TIMESTAMP WITH TIME ZONE NOT NULL
);
