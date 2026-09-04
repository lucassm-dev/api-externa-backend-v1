package com.apiexternabackend.config;

import com.apiexternabackend.resources.exceptions.StandardError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        boolean tokenExpirado = Boolean.TRUE.equals(request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_TOKEN_EXPIRADO));

        String codigo = tokenExpirado ? "AUT-006" : "AUT-005";
        String mensagem = tokenExpirado
                ? "Token expirado. Faça login novamente."
                : "Token ausente ou inválido.";

        StandardError erro = new StandardError(
                Instant.now(), HttpStatus.UNAUTHORIZED.value(), codigo,
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), mensagem, request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(erro));
    }
}