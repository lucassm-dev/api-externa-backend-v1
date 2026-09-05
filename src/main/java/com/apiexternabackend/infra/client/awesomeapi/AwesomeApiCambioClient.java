package com.apiexternabackend.infra.client.awesomeapi;

import com.apiexternabackend.config.FeignConfig;
import com.apiexternabackend.infra.client.awesomeapi.dtos.AwesomeApiCotacaoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(
    name = "awesomeapi-cambio-client",
    url = "${awesomeapi.cambio.url:https://economia.awesomeapi.com.br}",
    configuration = FeignConfig.class
)
public interface AwesomeApiCambioClient {

    @GetMapping("/json/last/USD-BRL")
    Map<String, AwesomeApiCotacaoDTO> buscarUsdBrl();
}