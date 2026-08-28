package com.apiexternabackend.infra.client.cvm;

import com.apiexternabackend.config.CvmFeignConfig;
import com.apiexternabackend.infra.client.cvm.dto.CvmCorretoraResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "cvm-corretora-client",
    url = "${brasilapi.cvm.url:https://brasilapi.com.br/api/cvm/corretoras/v1}",
    configuration = CvmFeignConfig.class
)
public interface CvmCorretoraClient {

    @GetMapping("/{cnpj}")
    CvmCorretoraResponseDTO buscarPorCnpj(@PathVariable("cnpj") String cnpj);
}
