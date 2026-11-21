-- spec-19 (#22): quem fez qual ação administrativa, em quem ou em quê, quando e por quê
CREATE TABLE registro_administrativo (
    id            BIGSERIAL    PRIMARY KEY,
    admin_id      BIGINT       NOT NULL REFERENCES usuario (id),
    acao          VARCHAR(30)  NOT NULL,
    alvo_tipo     VARCHAR(20)  NOT NULL,
    alvo_id       BIGINT       NOT NULL,
    motivo        VARCHAR(255),
    registrado_em TIMESTAMP    NOT NULL
);
