-- spec-28 (#34): uso da API por usuário, sem corpo, query, token nem IP; guardado por 90 dias
CREATE TABLE registro_uso (
    id            BIGSERIAL    PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL REFERENCES usuario (id),
    metodo        VARCHAR(10)  NOT NULL,
    rota          VARCHAR(200) NOT NULL,
    status        INT          NOT NULL,
    registrado_em TIMESTAMP    NOT NULL
);

CREATE INDEX idx_registro_uso_registrado_em ON registro_uso (registrado_em);
