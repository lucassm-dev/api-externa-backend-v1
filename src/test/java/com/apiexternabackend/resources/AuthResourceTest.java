package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.resources.exceptions.CredenciaisInvalidasException;
import com.apiexternabackend.resources.exceptions.GlobalExceptionHandler;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import com.apiexternabackend.services.AutenticacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthResource.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthResourceTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AutenticacaoService service;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("@spec:AC-001 POST /auth/cadastro com dados válidos cria a conta e retorna 201")
    void deveCadastrar() throws Exception {
        when(service.cadastrar(any())).thenReturn(new InvestidorResponseDTO(1L, "João", "joao@email.com"));

        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthCadastroRequestDTO("João", "joao@email.com", "12345678901", "senha123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("joao@email.com"));
    }

    @Test
    @DisplayName("@spec:AC-002 POST /auth/cadastro com e-mail duplicado retorna 409")
    void deveRetornar409ParaEmailDuplicado() throws Exception {
        when(service.cadastrar(any())).thenThrow(new RecursoDuplicadoException("AUT-001", "E-mail já está em uso"));

        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthCadastroRequestDTO("João", "joao@email.com", "12345678901", "senha123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("AUT-001"));
    }

    @Test
    @DisplayName("@spec:AC-436 POST /auth/cadastro com senha fraca retorna 422")
    void deveRetornar422ParaSenhaForaDaPolitica() throws Exception {
        when(service.cadastrar(any())).thenThrow(new RegraVioladaException("AUT-008", "Senha deve ter no mínimo 8 caracteres..."));

        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthCadastroRequestDTO("João", "joao@email.com", "12345678901", "123"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AUT-008"));
    }

    @Test
    @DisplayName("@spec:AC-004 POST /auth/login com credenciais corretas retorna 200 com token")
    void deveLogar() throws Exception {
        when(service.login(any())).thenReturn(new AuthTokenResponseDTO("token-ficticio", "Bearer", Instant.now().plusSeconds(86400)));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO("joao@email.com", "senha123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-ficticio"))
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    @DisplayName("@spec:AC-437 POST /auth/login nunca expõe a senha no corpo da resposta")
    void naoDeveExporSenhaNoLogin() throws Exception {
        when(service.login(any())).thenReturn(new AuthTokenResponseDTO("token-ficticio", "Bearer", Instant.now().plusSeconds(86400)));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO("joao@email.com", "senha123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.hash").doesNotExist());
    }

    @Test
    @DisplayName("@spec:AC-005 POST /auth/login com credenciais erradas retorna 401")
    void deveRetornar401ParaCredenciaisErradas() throws Exception {
        when(service.login(any())).thenThrow(new CredenciaisInvalidasException("AUT-004", "E-mail ou senha inválidos."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO("joao@email.com", "errada"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-004"));
    }

    @Test
    @DisplayName("@spec:AC-503 POST /auth/login de investidor excluído retorna 401 com a mesma mensagem genérica")
    void deveRetornar401ParaInvestidorExcluido() throws Exception {
        when(service.login(any())).thenThrow(new CredenciaisInvalidasException("AUT-004", "E-mail ou senha inválidos."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO("excluido@email.com", "senha123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-004"))
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    }
}
