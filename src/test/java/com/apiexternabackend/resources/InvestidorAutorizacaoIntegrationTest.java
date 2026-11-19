package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorização horizontal com o SecurityFilterChain e JWT reais: o investidor A usa o
 * próprio token contra os dados do investidor B (issue #16).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvestidorAutorizacaoIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String tokenA;
    private Long idA;
    private String tokenB;
    private Long idB;

    @BeforeEach
    void cadastrarAeB() throws Exception {
        tokenA = cadastrarELogar("autzA" + System.nanoTime() + "@email.com");
        idA = idDoLogado(tokenA);
        tokenB = cadastrarELogar("autzB" + System.nanoTime() + "@email.com");
        idB = idDoLogado(tokenB);
    }

    @Test
    @DisplayName("@spec:AC-542 GET /investidores devolve só a conta do investidor logado")
    void listagemTrazSoAPropriaConta() throws Exception {
        mockMvc.perform(get("/investidores").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(idA));
    }

    @Test
    @DisplayName("@spec:AC-543 GET /investidores/{id} de outro investidor responde 404 AUT-003, igual a inexistente")
    void buscarOutroInvestidorRespondeComoInexistente() throws Exception {
        mockMvc.perform(get("/investidores/" + idB).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("AUT-003"))
                .andExpect(jsonPath("$.message").value("Investidor não encontrado: " + idB));
    }

    @Test
    @DisplayName("@spec:AC-544 DELETE /investidores/{id} de outro investidor responde 404 e a conta dele continua ativa")
    void excluirOutroInvestidorERecusadoENaoMudaNada() throws Exception {
        mockMvc.perform(delete("/investidores/" + idB).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("AUT-003"));

        mockMvc.perform(get("/investidores/" + idB).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idB));
    }

    private Long idDoLogado(String token) throws Exception {
        String json = mockMvc.perform(get("/investidores").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).at("/content/0/id").asLong();
    }

    private String cadastrarELogar(String email) throws Exception {
        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthCadastroRequestDTO("Investidor Teste", email, cpfUnico(), "senha123"))))
                .andExpect(status().isCreated());

        String respostaJson = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO(email, "senha123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(respostaJson, AuthTokenResponseDTO.class).getToken();
    }

    private String cpfUnico() {
        return String.format("%011d", System.nanoTime() % 100_000_000_000L);
    }
}
