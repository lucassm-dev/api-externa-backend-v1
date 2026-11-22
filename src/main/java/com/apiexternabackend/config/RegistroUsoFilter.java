package com.apiexternabackend.config;

import com.apiexternabackend.domains.RegistroUso;
import com.apiexternabackend.services.RegistroUsoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
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
        if (!(autenticacao != null && autenticacao.getPrincipal() instanceof InvestidorPrincipal principal)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            chain.doFilter(request, response);
        } catch (ServletException | RuntimeException erro) {
            // ASM-506: o 403 do @PreAuthorize sobe (embrulhado pelo DispatcherServlet) até o ExceptionTranslationFilter, antes deste filtro
            if (erro instanceof AccessDeniedException || erro.getCause() instanceof AccessDeniedException) {
                registrar(principal, request, HttpServletResponse.SC_FORBIDDEN);
            }
            throw erro;
        }
        registrar(principal, request, response.getStatus());
    }

    private void registrar(InvestidorPrincipal principal, HttpServletRequest request, int status) {
        registroUsoService.registrar(new RegistroUso(principal.id(), request.getMethod(), rota(request), status, LocalDateTime.now()));
    }

    // ASM-507: só rota de controller tem padrão; o resto ("/**" dos arquivos estáticos) nunca grava a URL digitada
    private String rota(HttpServletRequest request) {
        if (request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE) instanceof HandlerMethod) {
            return (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        }
        return "desconhecida";
    }
}
