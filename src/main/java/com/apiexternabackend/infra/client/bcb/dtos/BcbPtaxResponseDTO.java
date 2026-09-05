package com.apiexternabackend.infra.client.bcb.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BcbPtaxResponseDTO {

    private List<Cotacao> value;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cotacao {
        private BigDecimal cotacaoVenda;
        private String dataHoraCotacao;
    }
}