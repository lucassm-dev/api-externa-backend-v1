package com.apiexternabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ApiExternaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiExternaBackendApplication.class, args);
    }
}
