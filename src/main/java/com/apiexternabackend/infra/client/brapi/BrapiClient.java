package com.apiexternabackend.infra.client.brapi;

import com.apiexternabackend.config.FeignConfig;
import com.apiexternabackend.infra.client.brapi.dtos.BrapiResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "brapi-client",
    url = "${brapi.url:https://brapi.dev/api}",
    configuration = FeignConfig.class
)
public interface BrapiClient {

    @GetMapping("/quote/{ticker}")
    BrapiResponseDTO buscarCotacao(
            @PathVariable("ticker") String ticker,
            @RequestParam(value = "token", required = false) String token
    );
}
