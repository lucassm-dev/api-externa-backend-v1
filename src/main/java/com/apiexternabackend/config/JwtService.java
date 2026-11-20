package com.apiexternabackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class JwtService {

    private final JwtEncoder encoder;
    private final String emissor;
    private final long expiracaoMs;

    public JwtService(JwtEncoder encoder,
                      @Value("${jwt.issuer}") String emissor,
                      @Value("${jwt.expiration-ms}") long expiracaoMs) {
        this.encoder = encoder;
        this.emissor = emissor;
        this.expiracaoMs = expiracaoMs;
    }

    public Jwt gerar(Long investidorId) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(investidorId))
                .issuer(emissor)
                .issuedAt(agora)
                .expiresAt(agora.plusMillis(expiracaoMs))
                // ASM-461: papel fixo até a issue #19
                .claim("authorities", List.of("ROLE_INVESTIDOR"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
    }
}
