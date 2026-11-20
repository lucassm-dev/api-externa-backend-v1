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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired private JwtEncoder jwtEncoder;

    @Value("${jwt.issuer}")
    private String emissor;

    @Test
    @DisplayName("@spec:AC-439 Endpoint protegido sem token retorna 401 com código AUT-005")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/carteiras"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-005"));
    }

    @Test
    @DisplayName("@spec:AC-541 Resposta 401 declara charset UTF-8 e a mensagem acentuada chega íntegra")
    void deveRetornar401EmUtf8() throws Exception {
        var resposta = mockMvc.perform(get("/carteiras"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse();

        org.assertj.core.api.Assertions.assertThat(resposta.getContentType()).containsIgnoringCase("charset=UTF-8");
        org.assertj.core.api.Assertions.assertThat(new String(resposta.getContentAsByteArray(), StandardCharsets.UTF_8))
                .contains("Token ausente ou inválido.");
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

    @Test
    @DisplayName("@spec:AC-545 Token traz só sub, iss, iat, exp e authorities, sem e-mail nem CPF")
    void tokenNaoLevaDadoPessoal() throws Exception {
        String email = "semdado" + System.nanoTime() + "@email.com";
        Map<String, Object> claims = payload(cadastrarELogar(email));

        assertThat(claims).containsOnlyKeys("sub", "iss", "iat", "exp", "authorities");
        assertThat(claims.toString()).doesNotContain(email);
    }

    @Test
    @DisplayName("@spec:AC-546 Token vale 60 minutos por padrão e o exp bate com a expiração informada")
    void tokenValeSessentaMinutos() throws Exception {
        String email = "validade" + System.nanoTime() + "@email.com";
        cadastrar(email);
        String respostaJson = login(email);
        AuthTokenResponseDTO resposta = objectMapper.readValue(respostaJson, AuthTokenResponseDTO.class);
        Map<String, Object> claims = payload(resposta.getToken());

        long iat = ((Number) claims.get("iat")).longValue();
        long exp = ((Number) claims.get("exp")).longValue();
        assertThat(exp - iat).isEqualTo(3600);
        assertThat(resposta.getExpiraEm().getEpochSecond()).isEqualTo(exp);
    }

    @Test
    @DisplayName("@spec:AC-547 Token adulterado depois de assinado retorna 401 AUT-005")
    void tokenAdulteradoEhRecusado() throws Exception {
        String[] partes = cadastrarELogar("adulterado" + System.nanoTime() + "@email.com").split("\\.");
        Map<String, Object> claims = payload(String.join(".", partes));
        claims.put("sub", "999999");
        String payloadAlterado = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(claims));
        String adulterado = partes[0] + "." + payloadAlterado + "." + partes[2];

        mockMvc.perform(get("/carteiras").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-005"));
    }

    @Test
    @DisplayName("@spec:AC-548 Token com a mesma chave mas outro emissor retorna 401 AUT-005")
    void tokenDeOutroEmissorEhRecusado() throws Exception {
        Instant agora = Instant.now();
        String token = gerarToken("outro-sistema", agora, agora.plusSeconds(3600));

        mockMvc.perform(get("/carteiras").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-005"));
    }

    @Test
    @DisplayName("@spec:AC-549 Fora do perfil dev o Swagger não responde a documentação")
    void swaggerForaDoDevNaoResponde() throws Exception {
        for (String rota : List.of("/swagger-ui.html", "/v3/api-docs")) {
            int status = mockMvc.perform(get(rota)).andReturn().getResponse().getStatus();
            assertThat(status).as(rota).isIn(401, 404);
        }
    }

    @Test
    @DisplayName("@spec:AC-550 Classes da jjwt não estão no classpath")
    void jjwtForaDoClasspath() {
        assertThatThrownBy(() -> Class.forName("io.jsonwebtoken.Jwts"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private String cadastrarELogar(String email) throws Exception {
        cadastrar(email);
        return objectMapper.readValue(login(email), AuthTokenResponseDTO.class).getToken();
    }

    private void cadastrar(String email) throws Exception {
        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthCadastroRequestDTO("Investidor Teste", email, cpfUnico(), "senha123"))))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO(email, "senha123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String gerarTokenExpirado() {
        Instant passado = Instant.now().minusSeconds(3600);
        return gerarToken(emissor, passado.minusSeconds(60), passado);
    }

    private String gerarToken(String iss, Instant emitidoEm, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .issuer(iss)
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .claim("authorities", List.of("ROLE_INVESTIDOR"))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private Map<String, Object> payload(String token) throws Exception {
        byte[] json = Base64.getUrlDecoder().decode(token.split("\\.")[1]);
        return objectMapper.readValue(json, Map.class);
    }

    private String cpfUnico() {
        return String.format("%011d", System.nanoTime() % 100_000_000_000L);
    }

    private String cnpjUnico() {
        // nanoTime conta desde o boot: logo após ligar a máquina tem menos de 14 dígitos
        return String.format("%014d", System.nanoTime() % 100_000_000_000_000L);
    }
}