package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.AcaoRequestDTO;
import com.apiexternabackend.domains.dtos.AcaoResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.resources.exceptions.BusinessException;
import com.apiexternabackend.resources.exceptions.ExternalServiceException;
import com.apiexternabackend.resources.exceptions.GlobalExceptionHandler;
import com.apiexternabackend.resources.exceptions.ResourceNotFoundException;
import com.apiexternabackend.services.AcaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcaoResource.class)
@Import(GlobalExceptionHandler.class)
class AcaoResourceTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AcaoService service;
    @Autowired private ObjectMapper objectMapper;

    private AcaoResponseDTO buildResponse(String ticker, Mercado mercado, String moeda) {
        return new AcaoResponseDTO(1L, ticker, "Empresa", mercado, moeda,
                new BigDecimal("38.00"), LocalDateTime.now());
    }

    @Test
    @DisplayName("@spec:AC-201 POST /acoes com ticker BR salva e retorna 201 com cotação e dataHora")
    void deveCadastrarAcaoBR() throws Exception {
        when(service.cadastrar(any())).thenReturn(buildResponse("PETR4", Mercado.BR, "BRL"));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcaoRequestDTO("PETR4", Mercado.BR))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.moeda").value("BRL"))
                .andExpect(jsonPath("$.cotacaoAtual").isNumber())
                .andExpect(jsonPath("$.dataHoraCotacao").isNotEmpty());
    }

    @Test
    @DisplayName("@spec:AC-202 POST /acoes com ticker US retorna moeda USD")
    void deveCadastrarAcaoUS() throws Exception {
        when(service.cadastrar(any())).thenReturn(buildResponse("AAPL", Mercado.US, "USD"));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcaoRequestDTO("AAPL", Mercado.US))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moeda").value("USD"));
    }

    @Test
    @DisplayName("@spec:AC-203 POST /acoes com ticker inexistente retorna 422")
    void deveRejeitarTickerInexistente() throws Exception {
        when(service.cadastrar(any()))
                .thenThrow(new BusinessException("Ticker não encontrado na fonte BR: XXXX3"));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcaoRequestDTO("XXXX3", Mercado.BR))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("@spec:AC-206 GET /acoes retorna lista paginada")
    void deveListarAcoes() throws Exception {
        when(service.listar(any())).thenReturn(
                new PageImpl<>(List.of(buildResponse("PETR4", Mercado.BR, "BRL")), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/acoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticker").value("PETR4"));
    }

    @Test
    @DisplayName("@spec:AC-207 GET /acoes/ticker/{ticker} retorna dados da ação")
    void deveBuscarPorTicker() throws Exception {
        when(service.buscarPorTicker("PETR4")).thenReturn(buildResponse("PETR4", Mercado.BR, "BRL"));

        mockMvc.perform(get("/acoes/ticker/PETR4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("PETR4"));
    }

    @Test
    @DisplayName("@spec:AC-207 GET /acoes/ticker/{ticker} com ticker inexistente retorna 404")
    void deveRetornar404ParaTickerInexistente() throws Exception {
        when(service.buscarPorTicker("XXXX3")).thenThrow(new ResourceNotFoundException("Ação não encontrada: XXXX3"));

        mockMvc.perform(get("/acoes/ticker/XXXX3"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("@spec:AC-208 PUT /acoes/{id}/atualizar-cotacao retorna ação com nova cotação")
    void deveAtualizarCotacao() throws Exception {
        when(service.atualizarCotacao(1L)).thenReturn(buildResponse("PETR4", Mercado.BR, "BRL"));

        mockMvc.perform(put("/acoes/1/atualizar-cotacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataHoraCotacao").isNotEmpty());
    }

    @Test
    @DisplayName("@spec:AC-211 Limite de cota da fonte retorna 502 com mensagem específica")
    void deveMensagemEspecificaParaLimiteExcedido() throws Exception {
        when(service.cadastrar(any()))
                .thenThrow(new ExternalServiceException("Limite de requisições da fonte BR excedido"));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcaoRequestDTO("PETR4", Mercado.BR))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Limite")));
    }

    @Test
    @DisplayName("@spec:AC-424 DELETE /acoes/{ticker} com ação ativa retorna 204")
    void deveExcluirAcaoAtiva() throws Exception {
        mockMvc.perform(delete("/acoes/PETR4"))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(service).excluir("PETR4");
    }

    @Test
    @DisplayName("@spec:AC-425 DELETE /acoes/{ticker} com ticker inexistente retorna 404")
    void deveRetornar404AoExcluirAcaoInexistente() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Ação não encontrada: XXXX3"))
                .when(service).excluir("XXXX3");

        mockMvc.perform(delete("/acoes/XXXX3"))
                .andExpect(status().isNotFound());
    }
}
