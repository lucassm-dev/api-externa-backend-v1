package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.CarteiraRenomearDTO;
import com.apiexternabackend.domains.dtos.CarteiraRequestDTO;
import com.apiexternabackend.domains.dtos.CarteiraResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.resources.exceptions.GlobalExceptionHandler;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import com.apiexternabackend.services.CarteiraService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

    private CarteiraResponseDTO buildResponse() {
        return new CarteiraResponseDTO(1L, 1L, 1L, "XP", Mercado.BR, "BRL", "Carteira BR", true);
    }

    @Test
    @DisplayName("@spec:AC-301 POST /carteiras cria carteira vinculada a investidor e corretora")
    void deveCriarCarteira() throws Exception {
        when(service.criar(any())).thenReturn(buildResponse());

        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CarteiraRequestDTO(1L, 1L, Mercado.BR, "Carteira BR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mercado").value("BR"))
                .andExpect(jsonPath("$.investidorId").value(1));
    }

    @Test
    @DisplayName("@spec:AC-303 POST /carteiras com corretora inexistente retorna 404")
    void deveRetornar404ParaCorretoraNaoEncontrada() throws Exception {
        when(service.criar(any())).thenThrow(new RecursoNaoEncontradoException("COR-001", "Corretora não encontrada: 99"));

        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CarteiraRequestDTO(1L, 99L, Mercado.BR, "X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("@spec:AC-306 GET /carteiras?investidorId= filtra por investidor")
    void deveListarCarteirasPorInvestidor() throws Exception {
        when(service.listarPorInvestidor(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(buildResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/carteiras").param("investidorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].investidorId").value(1));
    }

    @Test
    @DisplayName("@spec:AC-307 PATCH /carteiras/{id} renomeia a carteira")
    void deveRenomearCarteira() throws Exception {
        CarteiraResponseDTO resp = new CarteiraResponseDTO(1L, 1L, 1L, "XP", Mercado.BR, "BRL", "Novo Nome", true);
        when(service.renomear(eq(1L), eq("Novo Nome"))).thenReturn(resp);

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
        when(service.criar(any())).thenReturn(buildResponse());

        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CarteiraRequestDTO(1L, 1L, Mercado.BR, "C1"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CarteiraRequestDTO(1L, 1L, Mercado.US, "C2"))))
                .andExpect(status().isCreated());
    }
}