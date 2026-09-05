package com.apiexternabackend.resources.exceptions;

import com.apiexternabackend.config.InvestidorPrincipal;
import com.apiexternabackend.domains.dtos.AcaoRequestDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.resources.AcaoResource;
import com.apiexternabackend.services.AcaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcaoResource.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AcaoService service;

    @BeforeEach
    void autenticarComoInvestidor() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new InvestidorPrincipal(1L, "joao@email.com"), null,
                List.of(new SimpleGrantedAuthority("ROLE_INVESTIDOR"))));
    }

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("@spec:AC-426 Exceção de negócio conhecida retorna payload padronizado")
    void deveRetornarPayloadPadronizadoParaExcecaoDeNegocio() throws Exception {
        when(service.cadastrar(any(), any()))
                .thenThrow(new RecursoDuplicadoException("ACA-002", "Ação já cadastrada com o ticker: PETR4"));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcaoRequestDTO("PETR4", Mercado.BR))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("ACA-002"))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Ação já cadastrada com o ticker: PETR4"))
                .andExpect(jsonPath("$.path").value("/acoes"));
    }

    @Test
    @DisplayName("@spec:AC-427 Erro de validação de payload lista os campos inválidos")
    void deveListarCamposInvalidosNaValidacao() throws Exception {
        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VAL-001"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='ticker')].message").value("Ticker é obrigatório"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='mercado')].message").value("Mercado é obrigatório (BR ou US)"));
    }

    @Test
    @DisplayName("@spec:AC-428 Exceção não mapeada retorna 500 genérico sem stacktrace nem detalhe interno")
    void deveRetornar500GenericoSemStacktraceParaExcecaoNaoMapeada() throws Exception {
        when(service.cadastrar(any(), any())).thenThrow(new IllegalStateException("erro interno de programação, detalhe sensível"));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcaoRequestDTO("PETR4", Mercado.BR))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("SYS-001"))
                .andExpect(jsonPath("$.message").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("erro interno de programação"))))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("IllegalStateException"))));
    }
}