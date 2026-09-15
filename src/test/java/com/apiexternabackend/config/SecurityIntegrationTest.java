package com.apiexternabackend.config;

import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.dtos.CarteiraRenomearDTO;
import com.apiexternabackend.domains.dtos.CarteiraRequestDTO;
import com.apiexternabackend.domains.dtos.CarteiraResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.repositories.CorretoraRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes com o SecurityFilterChain real (sem addFilters=false) — os outros @WebMvcTest
 * do projeto desligam os filtros de segurança porque testam lógica de negócio, não
 * autenticação. Aqui é o contrário: o que se testa é exatamente o filtro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CorretoraRepository corretoraRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    @DisplayName("@spec:AC-439 Endpoint protegido sem token retorna 401 com código AUT-005")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/carteiras"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-005"));
    }

    @Test
    @DisplayName("@spec:AC-438 POST /auth/cadastro e /auth/login funcionam sem token")
    void deveFuncionarSemTokenNosEndpointsPublicos() throws Exception {
        String email = "publico" + System.nanoTime() + "@email.com";
        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthCadastroRequestDTO("Público", email, cpfUnico(), "senha123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO(email, "senha123"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("@spec:AC-440 @spec:AC-007 Endpoint protegido com token válido funciona; sem token, 401 antes de qualquer processamento")
    void deveFuncionarComTokenValidoENaoSemToken() throws Exception {
        String token = cadastrarELogar("valido" + System.nanoTime() + "@email.com");

        mockMvc.perform(get("/carteiras").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/carteiras"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("@spec:AC-441 Token expirado retorna 401 com código AUT-006, distinto de token ausente")
    void deveRetornar401ComCodigoDistintoParaTokenExpirado() throws Exception {
        String tokenExpirado = gerarTokenExpirado();

        mockMvc.perform(get("/carteiras").header("Authorization", "Bearer " + tokenExpirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-006"));
    }

    @Test
    @DisplayName("@spec:AC-006 Investidor B não acessa nem descobre a carteira do investidor A (404, não 403)")
    void deveIsolarCarteiraEntreInvestidores() throws Exception {
        String tokenA = cadastrarELogar("investidorA" + System.nanoTime() + "@email.com");
        String tokenB = cadastrarELogar("investidorB" + System.nanoTime() + "@email.com");

        Corretora corretora = new Corretora();
        corretora.setCnpj(cnpjUnico());
        corretora.setRazaoSocial("Corretora Teste");
        corretora.setValidadaNaCvm(true);
        corretora.setAtivo(true);
        corretora.setDataCadastro(LocalDateTime.now());
        corretora = corretoraRepository.save(corretora);

        String corpoCarteira = objectMapper.writeValueAsString(
                new CarteiraRequestDTO(corretora.getId(), Mercado.BR, "Carteira de A"));

        String respostaJson = mockMvc.perform(post("/carteiras").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCarteira))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CarteiraResponseDTO carteiraDeA = objectMapper.readValue(respostaJson, CarteiraResponseDTO.class);

        mockMvc.perform(patch("/carteiras/" + carteiraDeA.getId()).header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CarteiraRenomearDTO("Hackeada"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CAR-001"));
    }

    @Test
    @DisplayName("@spec:AC-442 Senha nunca aparece em log durante cadastro/login")
    void naoDeveLogarSenha() throws Exception {
        Logger raiz = (Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        raiz.addAppender(appender);

        try {
            cadastrarELogar("semlogdesenha" + System.nanoTime() + "@email.com");
        } finally {
            raiz.detachAppender(appender);
        }

        boolean vazouSenha = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(msg -> msg.contains("senha123"));
        org.assertj.core.api.Assertions.assertThat(vazouSenha).isFalse();
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

    private String gerarTokenExpirado() {
        SecretKey chave = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant passado = Instant.now().minusSeconds(3600);
        return Jwts.builder()
                .subject("1")
                .claim("email", "expirado@email.com")
                .issuedAt(Date.from(passado.minusSeconds(60)))
                .expiration(Date.from(passado))
                .signWith(chave)
                .compact();
    }

    private String cpfUnico() {
        return String.format("%011d", System.nanoTime() % 100_000_000_000L);
    }

    private String cnpjUnico() {
        // nanoTime conta desde o boot: logo após ligar a máquina tem menos de 14 dígitos
        return String.format("%014d", System.nanoTime() % 100_000_000_000_000L);
    }
}