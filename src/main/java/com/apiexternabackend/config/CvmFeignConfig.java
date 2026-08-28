package com.apiexternabackend.config;

import com.apiexternabackend.resources.exceptions.ExternalServiceException;
import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class CvmFeignConfig {

    public static final String CNPJ_NAO_ENCONTRADO = "CVM_CNPJ_NAO_ENCONTRADO";

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 404) return new ExternalServiceException(CNPJ_NAO_ENCONTRADO);
            return new ExternalServiceException("Erro ao consultar CVM: HTTP " + response.status());
        };
    }
}
