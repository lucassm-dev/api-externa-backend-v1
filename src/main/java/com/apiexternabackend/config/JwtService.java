package com.apiexternabackend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(@Value("${jwt.secret}") String segredo,
                       @Value("${jwt.expiration-ms}") long expiracaoMs) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    public String gerar(Long investidorId, String email) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(investidorId))
                .claim("email", email)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(expiracaoMs)))
                .signWith(chave)
                .compact();
    }

    public Instant expiracaoDe(String token) {
        return validar(token).getExpiration().toInstant();
    }

    /**
     * @throws io.jsonwebtoken.ExpiredJwtException token expirado
     * @throws io.jsonwebtoken.JwtException token ausente/malformado/assinatura inválida
     */
    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long investidorIdDe(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String emailDe(Claims claims) {
        return claims.get("email", String.class);
    }
}