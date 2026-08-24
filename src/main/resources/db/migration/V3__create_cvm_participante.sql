CREATE TABLE cvm_participante (
    cnpj              VARCHAR(14)  PRIMARY KEY,
    nome_empresa      VARCHAR(255) NOT NULL,
    tipo_participante VARCHAR(100) NOT NULL,
    situacao          VARCHAR(50)  NOT NULL,
    data_base         DATE         NOT NULL
);

CREATE INDEX idx_cvm_participante_data_base ON cvm_participante (data_base);
