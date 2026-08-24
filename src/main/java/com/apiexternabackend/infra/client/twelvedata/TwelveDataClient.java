package com.apiexternabackend.infra.client.twelvedata;

import com.apiexternabackend.config.FeignConfig;
import com.apiexternabackend.infra.client.twelvedata.dtos.TwelveDataResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "twelvedata-client",
    url = "${twelvedata.url:https://api.twelvedata.com}",
    configuration = FeignConfig.class
)
public interface TwelveDataClient {

    @GetMapping("/price")
    TwelveDataResponseDTO buscarCotacao(
            @RequestParam("symbol") String symbol,
            @RequestParam("apikey") String apiKey
    );
}
