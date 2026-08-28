package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.CorretoraRequestDTO;
import com.apiexternabackend.domains.dtos.CorretoraResponseDTO;
import com.apiexternabackend.resources.exceptions.BusinessException;
import com.apiexternabackend.resources.exceptions.DuplicateResourceException;
import com.apiexternabackend.resources.exceptions.GlobalExceptionHandler;
import com.apiexternabackend.resources.exceptions.ResourceNotFoundException;
import com.apiexternabackend.services.CorretoraService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CorretoraResource.class)
@Import(GlobalExceptionHandler.class)
class CorretoraResourceTest {

    private static final String CNPJ_VALIDO = "02332886000104";

    @Autowired private MockMvc mockMvc;
    @MockBean private CorretoraService service;
    @Autowired private ObjectMapper objectMapper;

    private CorretoraResponseDTO buildResponse() {
        CorretoraResponseDTO r = new CorretoraResponseDTO();
        r.setId(1L);
        r.setCnpj(CNPJ_VALIDO);
        r.setRazaoSocial("XP INVESTIMENTOS");
        r.setValidadaNaCvm(true);
        r.setDataBaseCvm(LocalDate.now());
        return r;
    }

    @Test
    @DisplayName("@spec:AC-102 POST /corretoras com CNPJ válido e autorizado persiste e retorna 201")
    void deveCadastrarCorretoraValida() throws Exception {
        when(service.cadastrar(any())).thenReturn(buildResponse());

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorretoraRequestDTO(CNPJ_VALIDO))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value(CNPJ_VALIDO))
                .andExpect(jsonPath("$.dataBaseCvm").isNotEmpty());
    }

    @Test
    @DisplayName("@spec:AC-104 POST /corretoras com CNPJ duplicado retorna 409")
    void deveRetornar409ParaCnpjDuplicado() throws Exception {
        when(service.cadastrar(any())).thenThrow(new DuplicateResourceException("CNPJ duplicado"));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorretoraRequestDTO(CNPJ_VALIDO))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("@spec:AC-105 POST /corretoras com corretora não autorizada retorna 422")
    void deveRetornar422ParaCorretoraNaoAutorizada() throws Exception {
        when(service.cadastrar(any())).thenThrow(new BusinessException("Corretora não autorizada na CVM"));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorretoraRequestDTO(CNPJ_VALIDO))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Corretora não autorizada na CVM"));
    }

    @Test
    @DisplayName("@spec:AC-106 POST /corretoras com falha na CVM retorna mensagem de falha")
    void deveRetornarMensagemDeFalhaQuandoCvmIndisponivel() throws Exception {
        when(service.cadastrar(any()))
                .thenThrow(new BusinessException("Não foi possível verificar a autorização na CVM"));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorretoraRequestDTO(CNPJ_VALIDO))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("verificar")));
    }

    @Test
    @DisplayName("@spec:AC-108 GET /corretoras retorna lista paginada")
    void deveListarCorretoras() throws Exception {
        when(service.listar(any())).thenReturn(
                new PageImpl<>(List.of(buildResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/corretoras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cnpj").value(CNPJ_VALIDO));
    }

    @Test
    @DisplayName("@spec:AC-109 GET /corretoras/{id} com id existente retorna dados")
    void deveBuscarPorId() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/corretoras/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("@spec:AC-109 GET /corretoras/{id} com id inexistente retorna 404")
    void deveRetornar404ParaIdInexistente() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Corretora não encontrada: 99"));

        mockMvc.perform(get("/corretoras/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("@spec:AC-110 GET /corretoras/cnpj/{cnpj} retorna corretora pelo CNPJ")
    void deveBuscarPorCnpj() throws Exception {
        when(service.buscarPorCnpj(CNPJ_VALIDO)).thenReturn(buildResponse());

        mockMvc.perform(get("/corretoras/cnpj/" + CNPJ_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value(CNPJ_VALIDO));
    }

    @Test
    @DisplayName("@spec:AC-420 DELETE /corretoras/{id} com corretora ativa retorna 204")
    void deveExcluirCorretoraAtiva() throws Exception {
        mockMvc.perform(delete("/corretoras/1"))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(service).excluir(1L);
    }

    @Test
    @DisplayName("@spec:AC-421 DELETE /corretoras/{id} com id inexistente retorna 404")
    void deveRetornar404AoExcluirCorretoraInexistente() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Corretora não encontrada: 99"))
                .when(service).excluir(99L);

        mockMvc.perform(delete("/corretoras/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("@spec:AC-111 Fonte externa indisponível retorna 502 com erro tratado")
    void deveRetornar502QuandoFonteExternaIndisponivel() throws Exception {
        when(service.cadastrar(any()))
                .thenThrow(new com.apiexternabackend.resources.exceptions.ExternalServiceException(
                        "Serviço de CNPJ indisponível"));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorretoraRequestDTO(CNPJ_VALIDO))))
                .andExpect(status().isBadGateway());
    }
}
