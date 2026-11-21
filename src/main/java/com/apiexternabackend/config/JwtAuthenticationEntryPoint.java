package com.apiexternabackend.config;

import org.springframework.security.authentication.DisabledException;
import com.apiexternabackend.resources.exceptions.StandardError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        boolean tokenExpirado = expirado(authException);
        boolean bloqueado = authException instanceof DisabledException;

        String codigo = bloqueado ? "AUT-010" : tokenExpirado ? "AUT-006" : "AUT-005";
        String mensagem = bloqueado ? "Conta bloqueada. Procure o suporte."
                : tokenExpirado ? "Token expirado. Faça login novamente."
                : "Token ausente ou inválido.";

        StandardError erro = new StandardError(
                Instant.now(), HttpStatus.UNAUTHORIZED.value(), codigo,
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), mensagem, request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(erro));
    }

    // ASM-464: o Spring só distingue expirado pelo texto do erro do validador ("Jwt expired at ...")
    private static boolean expirado(AuthenticationException e) {
        return e.getCause() instanceof JwtValidationException validacao
                && validacao.getErrors().stream()
                .anyMatch(erro -> erro.getDescription() != null && erro.getDescription().contains("expired"));
    }
}