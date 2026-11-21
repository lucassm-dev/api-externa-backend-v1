package com.apiexternabackend.resources.exceptions;

import com.apiexternabackend.config.InvestidorPrincipal;
import com.apiexternabackend.domains.dtos.CarteiraRequestDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.resources.CarteiraResource;
import com.apiexternabackend.services.CarteiraService;
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

// spec-21: POST /acoes saiu; o handler é exercitado pelo POST /carteiras
@WebMvcTest(CarteiraResource.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CarteiraService service;

    @BeforeEach
    void autenticarComoInvestidor() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new InvestidorPrincipal(1L), null,
                List.of(new SimpleGrantedAuthority("ROLE_INVESTIDOR"))));
    }

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("@spec:AC-426 Exceção de negócio conhecida retorna payload padronizado")
    void deveRetornarPayloadPadronizadoParaExcecaoDeNegocio() throws Exception {
        when(service.criar(any(), any()))
                .thenThrow(new RecursoNaoEncontradoException("COR-001", "Corretora não encontrada: 10"));

        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CarteiraRequestDTO(10L, Mercado.BR, "Carteira"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.codigo").value("COR-001"))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Corretora não encontrada: 10"))
                .andExpect(jsonPath("$.path").value("/carteiras"));
    }

    @Test
    @DisplayName("@spec:AC-427 Erro de validação de payload lista os campos inválidos")
    void deveListarCamposInvalidosNaValidacao() throws Exception {
        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VAL-001"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='nome')].message").value("Nome da carteira é obrigatório"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='mercado')].message").value("Mercado é obrigatório (BR ou US)"));
    }

    @Test
    @DisplayName("@spec:AC-428 Exceção não mapeada retorna 500 genérico sem stacktrace nem detalhe interno")
    void deveRetornar500GenericoSemStacktraceParaExcecaoNaoMapeada() throws Exception {
        when(service.criar(any(), any())).thenThrow(new IllegalStateException("erro interno de programação, detalhe sensível"));

        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CarteiraRequestDTO(10L, Mercado.BR, "Carteira"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("SYS-001"))
                .andExpect(jsonPath("$.message").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("erro interno de programação"))))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("IllegalStateException"))));
    }
}