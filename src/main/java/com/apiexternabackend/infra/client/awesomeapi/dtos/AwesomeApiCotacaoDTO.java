package com.apiexternabackend.infra.client.awesomeapi.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwesomeApiCotacaoDTO {

    private String ask;
    private String timestamp;
}
