package com.apiexternabackend.config;

import com.apiexternabackend.domains.RegistroUso;
import com.apiexternabackend.services.RegistroUsoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * spec-28: registra cada requisição autenticada. Fica depois do AuthorizationFilter, então
 * requisição sem token ou com token inválido nunca chega aqui. Sem @Component de propósito,
 * para não ser registrado também como filtro do servlet, fora da cadeia de segurança.
 */
@RequiredArgsConstructor
class RegistroUsoFilter extends OncePerRequestFilter {

    private final RegistroUsoService registroUsoService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        chain.doFilter(request, response);
        if (autenticacao != null && autenticacao.getPrincipal() instanceof InvestidorPrincipal principal) {
            Object rota = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            registroUsoService.registrar(new RegistroUso(
                    principal.id(), request.getMethod(), String.valueOf(rota), response.getStatus(), LocalDateTime.now()));
        }
    }
}
