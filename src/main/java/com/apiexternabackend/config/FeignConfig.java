package com.apiexternabackend.config;

import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            // 4xx: deixa propagar como FeignException para cada adapter tratar com contexto
            if (response.status() < 500) {
                return defaultDecoder.decode(methodKey, response);
            }
            return new IntegracaoExternaException("EXT-010",
                    "Serviço externo indisponível (HTTP " + response.status() + ")", false);
        };
    }
}
