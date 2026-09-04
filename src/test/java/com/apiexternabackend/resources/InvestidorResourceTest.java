package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.InvestidorRequestDTO;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
import com.apiexternabackend.resources.exceptions.GlobalExceptionHandler;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import com.apiexternabackend.services.InvestidorService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvestidorResource.class)
@Import(GlobalExceptionHandler.class)
class InvestidorResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvestidorService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("@spec:AC-051 POST /investidores com dados válidos cria e retorna 201")
    void deveCadastrarInvestidor() throws Exception {
        InvestidorRequestDTO req = new InvestidorRequestDTO("João", "joao@email.com", "12345678901");
        InvestidorResponseDTO resp = new InvestidorResponseDTO(1L, "João", "joao@email.com");
        when(service.cadastrar(any())).thenReturn(resp);

        mockMvc.perform(post("/investidores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("joao@email.com"));
    }

    @Test
    @DisplayName("@spec:AC-053 POST /investidores com e-mail inválido retorna 400")
    void deveRejeitarEmailInvalido() throws Exception {
        InvestidorRequestDTO req = new InvestidorRequestDTO("João", "nao-e-email", "12345678901");

        mockMvc.perform(post("/investidores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    @DisplayName("@spec:AC-053 POST /investidores com CPF mal formatado retorna 400")
    void deveRejeitarCpfMalFormatado() throws Exception {
        InvestidorRequestDTO req = new InvestidorRequestDTO("João", "joao@email.com", "123");

        mockMvc.perform(post("/investidores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("cpf"));
    }

    @Test
    @DisplayName("@spec:AC-054 GET /investidores retorna lista paginada")
    void deveListarInvestidores() throws Exception {
        InvestidorResponseDTO resp = new InvestidorResponseDTO(1L, "João", "joao@email.com");
        when(service.listar(any())).thenReturn(new PageImpl<>(List.of(resp), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/investidores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    @DisplayName("@spec:AC-055 GET /investidores/{id} com id existente retorna dados")
    void deveBuscarPorId() throws Exception {
        InvestidorResponseDTO resp = new InvestidorResponseDTO(1L, "João", "joao@email.com");
        when(service.buscarPorId(1L)).thenReturn(resp);

        mockMvc.perform(get("/investidores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("@spec:AC-055 GET /investidores/{id} com id inexistente retorna 404")
    void deveRetornar404ParaIdInexistente() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("AUT-003", "Investidor não encontrado: 99"));

        mockMvc.perform(get("/investidores/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("@spec:AC-052 POST /investidores com e-mail duplicado retorna 409")
    void deveRetornar409ParaEmailDuplicado() throws Exception {
        InvestidorRequestDTO req = new InvestidorRequestDTO("João", "joao@email.com", "12345678901");
        when(service.cadastrar(any())).thenThrow(new RecursoDuplicadoException("AUT-001", "E-mail já está em uso"));

        mockMvc.perform(post("/investidores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("@spec:AC-422 DELETE /investidores/{id} com investidor ativo retorna 204")
    void deveExcluirInvestidorAtivo() throws Exception {
        mockMvc.perform(delete("/investidores/1"))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(service).excluir(1L);
    }

    @Test
    @DisplayName("@spec:AC-423 DELETE /investidores/{id} com id inexistente retorna 404")
    void deveRetornar404AoExcluirInvestidorInexistente() throws Exception {
        org.mockito.Mockito.doThrow(new RecursoNaoEncontradoException("AUT-003", "Investidor não encontrado: 99"))
                .when(service).excluir(99L);

        mockMvc.perform(delete("/investidores/99"))
                .andExpect(status().isNotFound());
    }
}
