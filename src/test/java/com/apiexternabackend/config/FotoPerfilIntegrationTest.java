package com.apiexternabackend.config;

import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-25 (#30): foto de perfil, conforme ADR-017. Feita em TDD (Aula 15).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FotoPerfilIntegrationTest {

    private static final int DOIS_MB = 2 * 1024 * 1024;
    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("@spec:AC-633 Arquivo acima de 2 MB é recusado com AUT-016")
    void arquivoAcimaDoLimite() throws Exception {
        String token = token(cadastrar());

        enviar(token, png(DOIS_MB + 1))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AUT-016"));
        foto(token).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("@spec:AC-634 Conteúdo que não é JPEG, PNG nem WebP é recusado com AUT-017, mesmo declarado como imagem")
    void formatoInvalido() throws Exception {
        String token = token(cadastrar());
        byte[] pdf = "%PDF-1.4 conteudo qualquer".getBytes();

        enviar(token, pdf)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AUT-017"));
        foto(token).andExpect(status().isNotFound());
    }

    private static byte[] png(int tamanho) {
        return Arrays.copyOf(ASSINATURA_PNG, tamanho);
    }

    private ResultActions enviar(String token, byte[] conteudo) throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.png", MediaType.IMAGE_PNG_VALUE, conteudo);
        return mockMvc.perform(multipart(HttpMethod.PUT, "/auth/me/foto").file(arquivo)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions foto(String token) throws Exception {
        return mockMvc.perform(get("/auth/me/foto").header("Authorization", "Bearer " + token));
    }

    private String token(String email) throws Exception {
        String json = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO(email, "senha123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, AuthTokenResponseDTO.class).getToken();
    }

    private String cadastrar() throws Exception {
        String email = "foto" + System.nanoTime() + "@email.com";
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthCadastroRequestDTO("Foto", email,
                                String.format("%011d", System.nanoTime() % 100_000_000_000L), "senha123"))))
                .andExpect(status().isCreated());
        return email;
    }
}
