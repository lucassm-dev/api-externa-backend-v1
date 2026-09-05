package com.apiexternabackend.resources;

import com.apiexternabackend.config.InvestidorPrincipal;
import com.apiexternabackend.domains.dtos.CarteiraRenomearDTO;
import com.apiexternabackend.domains.dtos.CarteiraRequestDTO;
import com.apiexternabackend.domains.dtos.CarteiraResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.resources.exceptions.GlobalExceptionHandler;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CarteiraResource.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CarteiraResourceTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CarteiraService service;
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

    private CarteiraResponseDTO buildResponse() {
        return new CarteiraResponseDTO(1L, 1L, 1L, "XP", Mercado.BR, "BRL", "Carteira BR", true);
    }

    @Test
    @DisplayName("@spec:AC-301 POST /carteiras cria carteira vinculada ao investidor do token e à corretora")
    void deveCriarCarteira() throws Exception {
        when(service.criar(any(), eq(INVESTIDOR_ID))).thenReturn(buildResponse());

        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CarteiraRequestDTO(1L, Mercado.BR, "Carteira BR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mercado").value("BR"))
                .andExpect(jsonPath("$.investidorId").value(1));
    }

    @Test
    @DisplayName("@spec:AC-303 POST /carteiras com corretora inexistente retorna 404")
    void deveRetornar404ParaCorretoraNaoEncontrada() throws Exception {
        when(service.criar(any(), eq(INVESTIDOR_ID)))
                .thenThrow(new RecursoNaoEncontradoException("COR-001", "Corretora não encontrada: 99"));

        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CarteiraRequestDTO(99L, Mercado.BR, "X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("@spec:AC-306 GET /carteiras filtra pelo investidor do token")
    void deveListarCarteirasPorInvestidor() throws Exception {
        when(service.listarPorInvestidor(eq(INVESTIDOR_ID), any()))
                .thenReturn(new PageImpl<>(List.of(buildResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/carteiras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].investidorId").value(1));
    }

    @Test
    @DisplayName("@spec:AC-307 PATCH /carteiras/{id} renomeia a carteira")
    void deveRenomearCarteira() throws Exception {
        CarteiraResponseDTO resp = new CarteiraResponseDTO(1L, 1L, 1L, "XP", Mercado.BR, "BRL", "Novo Nome", true);
        when(service.renomear(eq(1L), eq("Novo Nome"), eq(INVESTIDOR_ID))).thenReturn(resp);

        mockMvc.perform(patch("/carteiras/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CarteiraRenomearDTO("Novo Nome"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Nome"));
    }

    @Test
    @DisplayName("@spec:AC-308 DELETE /carteiras/{id} exclui logicamente e retorna 204")
    void deveExcluirLogicamenteCarteira() throws Exception {
        mockMvc.perform(delete("/carteiras/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("@spec:AC-302 Múltiplas carteiras do mesmo investidor coexistem")
    void devePermitirMultiplasCarteiras() throws Exception {
        when(service.criar(any(), eq(INVESTIDOR_ID))).thenReturn(buildResponse());

        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CarteiraRequestDTO(1L, Mercado.BR, "C1"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CarteiraRequestDTO(1L, Mercado.US, "C2"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("@spec:AC-006 GET /carteiras/{id}/... (via renomear) de outro investidor retorna 404, não 403")
    void deveRetornar404AoTentarRenomearCarteiraDeOutroInvestidor() throws Exception {
        when(service.renomear(eq(1L), eq("Hack"), eq(INVESTIDOR_ID)))
                .thenThrow(new RecursoNaoEncontradoException("CAR-001", "Carteira não encontrada ou inativa: 1"));

        mockMvc.perform(patch("/carteiras/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CarteiraRenomearDTO("Hack"))))
                .andExpect(status().isNotFound());
    }
}
