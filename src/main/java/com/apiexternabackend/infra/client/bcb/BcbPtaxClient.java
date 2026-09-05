package com.apiexternabackend.infra.client.bcb;

import com.apiexternabackend.config.FeignConfig;
import com.apiexternabackend.infra.client.bcb.dtos.BcbPtaxResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "bcb-ptax-client",
    url = "${bcb.ptax.url:https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata}",
    configuration = FeignConfig.class
)
public interface BcbPtaxClient {

    @GetMapping("/CotacaoDolarDia(dataCotacao=@dataCotacao)")
    BcbPtaxResponseDTO buscarCotacaoDoDia(
            @RequestParam("@dataCotacao") String dataCotacaoEntreAspas,
            @RequestParam("$format") String format);
}
