package com.apiexternabackend.config;

import com.apiexternabackend.resources.exceptions.ExternalServiceException;
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
        return (methodKey, response) -> {
            String msg = "Erro na chamada externa [" + methodKey + "]: HTTP " + response.status();
            return new ExternalServiceException(msg);
        };
    }
}
