package com.apiexternabackend.infra.client.cep;

import com.apiexternabackend.config.FeignConfig;
import com.apiexternabackend.infra.client.cep.dto.CepResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "cep-client",
    url = "${viacep.url:https://viacep.com.br/ws}",
    configuration = FeignConfig.class
)
public interface CepClient {

    @GetMapping("/{cep}/json/")
    CepResponseDTO buscarPorCep(@PathVariable("cep") String cep);
}
