package com.apiexternabackend.infra.client.coingecko;

import com.apiexternabackend.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(
    name = "coingecko-client",
    url = "${coingecko.url:https://api.coingecko.com/api/v3}",
    configuration = FeignConfig.class
)
public interface CoinGeckoClient {

    @GetMapping("/simple/price")
    Map<String, Map<String, BigDecimal>> buscarPreco(
            @RequestParam("ids") String ids,
            @RequestParam("vs_currencies") String vsCurrencies,
            @RequestParam("include_24hr_change") boolean include24hChange);
}