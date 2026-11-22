package com.apiexternabackend.config;

import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.dtos.TrocarSenhaRequestDTO;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-23 (#27): trocar senha no perfil com a senha atual.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrocarSenhaIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("@spec:AC-620 Senha atual errada é recusada e a senha não muda")
    void senhaAtualErrada() throws Exception {
        String email = cadastrar();
        trocar(token(login(email, "senha123")), "errada123", "novaSenha9")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AUT-014"));

        login(email, "senha123").andExpect(status().isOk());
        login(email, "novaSenha9").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("@spec:AC-621 Nova senha é gravada com BCrypt e passa a valer no login")
    void novaSenhaComBcrypt() throws Exception {
        String email = cadastrar();
        trocar(token(login(email, "senha123")), "senha123", "novaSenha9").andExpect(status().isOk());

        String hash = usuarioRepository.findByEmailAndSituacaoNot(email, SituacaoUsuario.EXCLUIDO).orElseThrow().getSenha();
        assertThat(hash).startsWith("$2").doesNotContain("novaSenha9");
        login(email, "senha123").andExpect(status().isUnauthorized());
        login(email, "novaSenha9").andExpect(status().isOk());
    }

    @Test
    @DisplayName("@spec:AC-622 Token emitido antes da troca recebe 401; o devolvido pela troca funciona")
    void tokenAnteriorCai() throws Exception {
        String email = cadastrar();
        String outroDispositivo = token(login(email, "senha123"));
        String esteDispositivo = token(login(email, "senha123"));
        Thread.sleep(1100); // iat tem precisão de segundos

        String novo = token(trocar(esteDispositivo, "senha123", "novaSenha9"));

        me(outroDispositivo).andExpect(status().isUnauthorized());
        me(esteDispositivo).andExpect(status().isUnauthorized());
        me(novo).andExpect(status().isOk());
    }

    @Test
    @DisplayName("@spec:AC-623 Nova senha fora da política é recusada com AUT-008")
    void novaSenhaForaDaPolitica() throws Exception {
        String email = cadastrar();
        trocar(token(login(email, "senha123")), "senha123", "curta")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AUT-008"));
        login(email, "senha123").andExpect(status().isOk());
    }

    @Test
    @DisplayName("@spec:AC-624 Sem token responde 401")
    void semToken() throws Exception {
        mockMvc.perform(put("/auth/senha").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrocarSenhaRequestDTO("senha123", "novaSenha9"))))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions trocar(String token, String atual, String nova) throws Exception {
        return mockMvc.perform(put("/auth/senha").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TrocarSenhaRequestDTO(atual, nova))));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO(email, senha))));
    }

    private String token(ResultActions resposta) throws Exception {
        String json = resposta.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, AuthTokenResponseDTO.class).getToken();
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token));
    }

    private String cadastrar() throws Exception {
        String email = "troca" + System.nanoTime() + "@email.com";
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthCadastroRequestDTO("Troca", email,
                                String.format("%011d", System.nanoTime() % 100_000_000_000L), "senha123"))))
                .andExpect(status().isCreated());
        return email;
    }
}
