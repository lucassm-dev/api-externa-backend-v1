package com.apiexternabackend.infra.client.brapi.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class BrapiResultDTO {

    private String symbol;
    private String shortName;
    private String longName;

    @JsonProperty("regularMarketPrice")
    private BigDecimal regularMarketPrice;

    @JsonProperty("regularMarketTime")
    private String regularMarketTime;

    private String currency;
}
