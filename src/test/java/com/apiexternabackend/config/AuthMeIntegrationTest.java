package com.apiexternabackend.config;

import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-17: GET /auth/me com SecurityFilterChain, JWT e banco reais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthMeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JwtService jwtService;

    @Test
    @DisplayName("@spec:AC-569 Investidor recebe id, nome, e-mail e papel INVESTIDOR do próprio cadastro")
    void investidorRecebeOsProprioDados() throws Exception {
        String email = emailUnico();
        Long id = cadastrar("Maria Me", email);

        me(logar(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nome").value("Maria Me"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.papel").value("INVESTIDOR"));
    }

    @Test
    @DisplayName("@spec:AC-570 Cada token devolve só os dados do próprio dono")
    void cadaTokenDevolveSoOProprioDono() throws Exception {
        String emailA = emailUnico();
        String emailB = emailUnico();
        Long idA = cadastrar("Investidor A", emailA);
        Long idB = cadastrar("Investidor B", emailB);

        me(logar(emailA)).andExpect(jsonPath("$.id").value(idA)).andExpect(jsonPath("$.email").value(emailA));
        me(logar(emailB)).andExpect(jsonPath("$.id").value(idB)).andExpect(jsonPath("$.email").value(emailB));
    }

    @Test
    @DisplayName("@spec:AC-571 ADMIN recebe id, e-mail e papel ADMIN com nome nulo")
    void adminRecebeDadosSemNome() throws Exception {
        String email = emailUnico();
        Usuario admin = usuarioRepository.save(new Usuario(null, email, "hash", Papel.ADMIN, SituacaoUsuario.ATIVO));
        String token = jwtService.gerar(admin.getId(), Papel.ADMIN).getTokenValue();

        me(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.papel").value("ADMIN"))
                .andExpect(jsonPath("$.nome").value(nullValue()));
    }

    @Test
    @DisplayName("@spec:AC-572 Sem token responde 401 AUT-005")
    void semTokenResponde401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-005"));
    }

    @Test
    @DisplayName("@spec:AC-573 Token de conta excluída responde 401 AUT-005")
    void contaExcluidaResponde401() throws Exception {
        String email = emailUnico();
        Long id = cadastrar("Vai Excluir", email);
        String token = logar(email);
        mockMvc.perform(delete("/investidores/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        me(token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-005"))
                .andExpect(jsonPath("$.message").value("Token ausente ou inválido."));
    }

    @Test
    @DisplayName("@spec:AC-574 Resposta tem só id, nome, email e papel, sem senha")
    void respostaNaoTrazSenha() throws Exception {
        String email = emailUnico();
        cadastrar("Sem Senha", email);

        String json = me(logar(email)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode corpo = objectMapper.readTree(json);

        assertThat(corpo.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "nome", "email", "papel");
        assertThat(json).doesNotContain("senha123").doesNotContain("$2a$");
    }

    @Test
    @DisplayName("@spec:AC-575 Cadastro e login continuam respondendo sem token")
    void cadastroELoginContinuamPublicos() throws Exception {
        String email = emailUnico();
        cadastrar("Publico", email);
        login(email).andExpect(status().isOk());
    }

    private Long cadastrar(String nome, String email) throws Exception {
        String json = mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthCadastroRequestDTO(nome, email, cpfUnico(), "senha123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO(email, "senha123"))));
    }

    private String logar(String email) throws Exception {
        String json = login(email).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, AuthTokenResponseDTO.class).getToken();
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token));
    }

    private static String emailUnico() {
        return "me" + System.nanoTime() + "@email.com";
    }

    private static String cpfUnico() {
        return String.format("%011d", System.nanoTime() % 100_000_000_000L);
    }
}
