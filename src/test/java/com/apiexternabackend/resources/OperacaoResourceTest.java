package com.apiexternabackend.resources;

import com.apiexternabackend.config.InvestidorPrincipal;
import com.apiexternabackend.domains.dtos.CarteiraAcaoResponseDTO;
import com.apiexternabackend.domains.dtos.OperacaoEditarDTO;
import com.apiexternabackend.domains.dtos.OperacaoRequestDTO;
import com.apiexternabackend.domains.dtos.OperacaoResponseDTO;
import com.apiexternabackend.domains.enums.TipoOperacao;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import com.apiexternabackend.resources.exceptions.GlobalExceptionHandler;
import com.apiexternabackend.services.ConsultaOperacaoService;
import com.apiexternabackend.services.OperacaoService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

@WebMvcTest(OperacaoResource.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OperacaoResourceTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private OperacaoService operacaoService;
    @MockBean private ConsultaOperacaoService consultaService;
    @Autowired private ObjectMapper objectMapper;

    private static final Long INVESTIDOR_ID = 1L;

    @BeforeEach
    void autenticarComoInvestidor() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new InvestidorPrincipal(INVESTIDOR_ID, "joao@email.com"), null,
                List.of(new SimpleGrantedAuthority("ROLE_INVESTIDOR"))));
    }

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    private OperacaoResponseDTO buildResponse(TipoOperacao tipo) {
        return new OperacaoResponseDTO(1L, 1L, "PETR4", tipo, 100,
                new BigDecimal("38"), new BigDecimal("3800"), LocalDateTime.now(), "BRL", List.of(), null, null);
    }

    @Test
    @DisplayName("@spec:AC-401 POST /operacoes/compra usa cotação da fonte e cria operação")
    void deveRegistrarCompra() throws Exception {
        when(operacaoService.comprar(any(), eq(INVESTIDOR_ID))).thenReturn(buildResponse(TipoOperacao.COMPRA));

        mockMvc.perform(post("/operacoes/compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacaoRequestDTO(1L, "PETR4", 100, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("COMPRA"))
                .andExpect(jsonPath("$.precoUnitario").isNumber());
    }

    @Test
    @DisplayName("@spec:AC-406 POST /operacoes/venda alimenta resultado realizado")
    void deveRegistrarVenda() throws Exception {
        when(operacaoService.vender(any(), eq(INVESTIDOR_ID))).thenReturn(buildResponse(TipoOperacao.VENDA));

        mockMvc.perform(post("/operacoes/venda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacaoRequestDTO(1L, "PETR4", 50, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("VENDA"));
    }

    @Test
    @DisplayName("@spec:AC-404 POST /operacoes/venda acima da posição retorna 422")
    void deveRejeitarVendaAcimaDataPosicao() throws Exception {
        when(operacaoService.vender(any(), eq(INVESTIDOR_ID)))
                .thenThrow(new RegraVioladaException("OPE-004", "Quantidade excede a posição atual"));

        mockMvc.perform(post("/operacoes/venda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacaoRequestDTO(1L, "PETR4", 999, null))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("@spec:AC-409 GET /operacoes retorna histórico do investidor do token")
    void deveRetornarHistoricoComCampos() throws Exception {
        OperacaoResponseDTO op = buildResponse(TipoOperacao.COMPRA);
        when(consultaService.historico(eq(INVESTIDOR_ID), any()))
                .thenReturn(new PageImpl<>(List.of(op), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/operacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dataHora").isNotEmpty())
                .andExpect(jsonPath("$.content[0].tipo").value("COMPRA"))
                .andExpect(jsonPath("$.content[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.content[0].quantidade").value(100))
                .andExpect(jsonPath("$.content[0].precoUnitario").isNumber())
                .andExpect(jsonPath("$.content[0].valorTotal").isNumber());
    }

    @Test
    @DisplayName("@spec:AC-410 GET /carteiras/{id}/posicoes retorna rentabilidade não realizada")
    void deveRetornarRentabilidadeNaoRealizada() throws Exception {
        CarteiraAcaoResponseDTO pos = new CarteiraAcaoResponseDTO(
                1L, "PETR4", "Petrobras", 100, new BigDecimal("38"),
                new BigDecimal("42"), LocalDateTime.now(), new BigDecimal("400"));
        when(consultaService.posicoes(1L, INVESTIDOR_ID)).thenReturn(List.of(pos));

        mockMvc.perform(get("/carteiras/1/posicoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rentabilidadeNaoRealizada").value(400));
    }

    @Test
    @DisplayName("@spec:AC-412 PUT /operacoes/{id} edita lançamento e recalcula posição")
    void deveEditarLancamento() throws Exception {
        when(operacaoService.editar(eq(1L), eq(50), any(), eq(INVESTIDOR_ID))).thenReturn(buildResponse(TipoOperacao.COMPRA));

        mockMvc.perform(put("/operacoes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OperacaoEditarDTO(50, null))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("@spec:AC-464 PUT /operacoes/{id} com preço zero ou negativo retorna 400")
    void deveRejeitarEdicaoComPrecoInvalido() throws Exception {
        mockMvc.perform(put("/operacoes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OperacaoEditarDTO(null, new BigDecimal("-5")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VAL-001"));
    }

    @Test
    @DisplayName("@spec:AC-413 DELETE /operacoes/{id} exclui lançamento e recalcula posição")
    void deveExcluirLancamento() throws Exception {
        mockMvc.perform(delete("/operacoes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("@spec:AC-458 POST /operacoes/compra com preço zero ou negativo retorna 400")
    void deveRejeitarPrecoZeroOuNegativo() throws Exception {
        mockMvc.perform(post("/operacoes/compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacaoRequestDTO(1L, "PETR4", 100, new BigDecimal("-10")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VAL-001"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("precoUnitario"));
    }

    @Test
    @DisplayName("@spec:AC-460 Resposta traz aviso quando o preço informado destoa muito da cotação")
    void deveTrazerAvisoDeDesvioNoCorpo() throws Exception {
        OperacaoResponseDTO comAviso = new OperacaoResponseDTO(1L, 1L, "PETR4", TipoOperacao.COMPRA, 100,
                new BigDecimal("4750.00"), new BigDecimal("475000.00"), LocalDateTime.now(), "BRL",
                List.of("O preço informado (4750.00) está 100x acima da cotação atual (47.50). Confirme se está correto."),
                null, null);
        when(operacaoService.comprar(any(), eq(INVESTIDOR_ID))).thenReturn(comAviso);

        mockMvc.perform(post("/operacoes/compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacaoRequestDTO(1L, "PETR4", 100, new BigDecimal("4750.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avisos[0]").value(org.hamcrest.Matchers.containsString("100x acima")));
    }

    @Test
    @DisplayName("@spec:AC-462 Resposta da operação informa a moeda do preço unitário")
    void deveInformarMoedaNaResposta() throws Exception {
        when(operacaoService.comprar(any(), eq(INVESTIDOR_ID))).thenReturn(buildResponse(TipoOperacao.COMPRA));

        mockMvc.perform(post("/operacoes/compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacaoRequestDTO(1L, "PETR4", 100, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moeda").value("BRL"));
    }
}