package com.apiexternabackend.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;

public class CvmFeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
