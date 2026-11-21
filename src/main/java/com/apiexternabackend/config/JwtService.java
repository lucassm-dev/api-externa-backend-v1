package com.apiexternabackend.config;

import com.apiexternabackend.domains.enums.Papel;
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

    public Jwt gerar(Long usuarioId, Papel papel) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuarioId))
                .issuer(emissor)
                .issuedAt(agora)
                .expiresAt(agora.plusMillis(expiracaoMs))
                // spec-16 (AC-558): papel real do usuário
                .claim("authorities", List.of("ROLE_" + papel.name()))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
    }
}
