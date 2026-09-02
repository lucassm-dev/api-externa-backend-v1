package com.apiexternabackend.infra.client.twelvedata.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TwelveDataResponseDTO {

    private String symbol;
    private String name;
    private String currency;

    @JsonProperty("price")
    private String price;

    @JsonProperty("datetime")
    private String datetime;

    private String status;
    private String message;

    public boolean isError() {
        return "error".equalsIgnoreCase(status);
    }

    public boolean isRateLimit() {
        return message != null && (message.contains("429") || message.toLowerCase().contains("rate limit")
                || message.toLowerCase().contains("quota"));
    }
}
