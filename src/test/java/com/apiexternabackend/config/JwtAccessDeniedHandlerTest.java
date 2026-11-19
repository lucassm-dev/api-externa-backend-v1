package com.apiexternabackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessDeniedHandlerTest {

    @Test
    @DisplayName("@spec:AC-541 Resposta 403 declara charset UTF-8")
    void deveRetornar403EmUtf8() throws Exception {
        var handler = new JwtAccessDeniedHandler(new ObjectMapper().registerModule(new JavaTimeModule()));
        var resposta = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest("GET", "/carteiras"), resposta, new AccessDeniedException("negado"));

        assertThat(resposta.getStatus()).isEqualTo(403);
        assertThat(resposta.getContentType()).containsIgnoringCase("charset=UTF-8");
    }
}
