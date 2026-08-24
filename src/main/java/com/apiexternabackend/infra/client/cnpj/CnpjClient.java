package com.apiexternabackend.infra.client.cnpj;

import com.apiexternabackend.config.FeignConfig;
import com.apiexternabackend.infra.client.cnpj.dto.CnpjResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "cnpj-client",
    url = "${brasilia-api.url:https://brasilapi.com.br/api/cnpj/v1}",
    configuration = FeignConfig.class
)
public interface CnpjClient {

    @GetMapping("/{cnpj}")
    CnpjResponseDTO buscarPorCnpj(@PathVariable("cnpj") String cnpj);
}
