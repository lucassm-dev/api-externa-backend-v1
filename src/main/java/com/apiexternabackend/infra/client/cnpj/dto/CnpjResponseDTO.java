package com.apiexternabackend.infra.client.cnpj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CnpjResponseDTO {

    private String cnpj;

    @JsonProperty("razao_social")
    private String razaoSocial;

    @JsonProperty("nome_fantasia")
    private String nomeFantasia;

    private String email;
    private String telefone;

    @JsonProperty("situacao_cadastral")
    private String situacaoCadastral;

    private CnpjEnderecoDTO endereco;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CnpjEnderecoDTO {

        @JsonProperty("cep")
        private String cep;

        @JsonProperty("logradouro")
        private String logradouro;

        @JsonProperty("numero")
        private String numero;

        @JsonProperty("complemento")
        private String complemento;

        @JsonProperty("bairro")
        private String bairro;

        @JsonProperty("municipio")
        private String municipio;

        @JsonProperty("uf")
        private String uf;
    }
}
