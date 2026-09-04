package com.apiexternabackend.domains.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponseDTO {

    private String token;
    private String tipo;
    private Instant expiraEm;
}