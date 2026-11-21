package com.apiexternabackend.config;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.AcaoRepository;
import com.apiexternabackend.repositories.CarteiraAcaoRepository;
import com.apiexternabackend.repositories.CarteiraRepository;
import com.apiexternabackend.repositories.CorretoraRepository;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-18: gestão de usuários pelo ADMIN com SecurityFilterChain, JWT e banco reais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GestaoUsuariosIntegrationTest {

    private static final String SENHA = "senha123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private JwtDecoder jwtDecoder;
    @Autowired private TransactionTemplate transacao;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private InvestidorRepository investidorRepository;
    @Autowired private CorretoraRepository corretoraRepository;
    @Autowired private CarteiraRepository carteiraRepository;
    @Autowired private AcaoRepository acaoRepository;
    @Autowired private CarteiraAcaoRepository carteiraAcaoRepository;

    private Usuario admin;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        admin = usuario(Papel.ADMIN, SituacaoUsuario.ATIVO);
        tokenAdmin = token(admin);
    }

    @Test
    @DisplayName("@spec:AC-576 Lista usuários com indicadores e filtro por situação")
    void listaComIndicadoresEFiltro() throws Exception {
        Investidor comPosicao = investidor(SituacaoUsuario.ATIVO, "Com Posicao");
        Carteira carteira = carteira(comPosicao);
        carteira(comPosicao);
        posicao(carteira, "PTRX" + (System.nanoTime() % 1000));
        Investidor bloqueado = investidor(SituacaoUsuario.BLOQUEADO, "Bloqueado");
        Usuario excluido = usuario(Papel.INVESTIDOR, SituacaoUsuario.EXCLUIDO);

        List<JsonNode> todos = listar(null);
        JsonNode linha = porId(todos, comPosicao.getId());
        assertThat(linha.get("nome").asText()).isEqualTo("Com Posicao");
        assertThat(linha.get("papel").asText()).isEqualTo("INVESTIDOR");
        assertThat(linha.get("situacao").asText()).isEqualTo("ATIVO");
        assertThat(linha.get("carteirasAtivas").asLong()).isEqualTo(2);
        assertThat(linha.get("temPosicaoAberta").asBoolean()).isTrue();
        assertThat(porId(todos, bloqueado.getId()).get("carteirasAtivas").asLong()).isZero();
        assertThat(porId(todos, admin.getId()).get("nome").isNull()).isTrue();
        assertThat(ids(todos)).doesNotContain(excluido.getId());

        List<JsonNode> soBloqueados = listar("BLOQUEADO");
        assertThat(ids(soBloqueados)).contains(bloqueado.getId()).doesNotContain(comPosicao.getId(), admin.getId());
        assertThat(soBloqueados).allMatch(u -> u.get("situacao").asText().equals("BLOQUEADO"));
    }

    @Test
    @DisplayName("@spec:AC-577 Listagem não mostra conteúdo financeiro nem senha")
    void listagemSemConteudoFinanceiro() throws Exception {
        Investidor investidor = investidor(SituacaoUsuario.ATIVO, "Rico");
        String ticker = "SIGL" + (System.nanoTime() % 1000);
        posicao(carteira(investidor), ticker);

        String json = mockMvc.perform(get("/admin/usuarios?size=1000").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(json).doesNotContain(ticker).doesNotContain("$2")
                .doesNotContain("\"ticker\"").doesNotContain("\"quantidade\"").doesNotContain("\"precoMedio\"")
                .doesNotContain("\"valor").doesNotContain("\"operac").doesNotContain("\"senha\"");
        assertThat(objectMapper.readTree(json).get("content").get(0).fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "email", "nome", "papel", "situacao", "motivoBloqueio",
                        "carteirasAtivas", "temPosicaoAberta");
    }

    @Test
    @DisplayName("@spec:AC-578 Só o ADMIN gerencia usuários")
    void soAdminGerencia() throws Exception {
        Usuario alvo = usuario(Papel.INVESTIDOR, SituacaoUsuario.ATIVO);
        String tokenInvestidor = token(usuario(Papel.INVESTIDOR, SituacaoUsuario.ATIVO));

        for (MockHttpServletRequestBuilder rota : rotas(alvo.getId())) {
            mockMvc.perform(rota.header("Authorization", "Bearer " + tokenInvestidor))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("AUT-007"));
        }
        for (MockHttpServletRequestBuilder rota : rotas(alvo.getId())) {
            mockMvc.perform(rota).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("AUT-005"));
        }
        for (MockHttpServletRequestBuilder rota : rotas(alvo.getId())) {
            int status = mockMvc.perform(rota.header("Authorization", "Bearer " + tokenAdmin))
                    .andReturn().getResponse().getStatus();
            assertThat(status).isBetween(200, 299);
        }
    }

    private List<MockHttpServletRequestBuilder> rotas(Long alvoId) throws Exception {
        return List.of(
                get("/admin/usuarios"),
                post("/admin/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", emailUnico(), "senha", SENHA))),
                patch("/admin/usuarios/" + alvoId + "/bloquear").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("motivo", "teste"))),
                patch("/admin/usuarios/" + alvoId + "/desbloquear"));
    }

    @Test
    @DisplayName("@spec:AC-579 ADMIN criado consegue entrar")
    void adminCriadoEntra() throws Exception {
        String email = emailUnico();
        mockMvc.perform(post("/admin/usuarios").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email", email, "senha", SENHA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.papel").value("ADMIN"))
                .andExpect(jsonPath("$.situacao").value("ATIVO"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        String resposta = login(email).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(resposta).get("token").asText();
        assertThat(jwtDecoder.decode(token).getClaimAsStringList("authorities")).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("@spec:AC-580 Criação recusa e-mail em uso e senha fraca")
    void criacaoRecusaDuplicadoESenhaFraca() throws Exception {
        long antes = usuarioRepository.count();
        criarAdmin(admin.getEmail(), SENHA)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("AUT-001"));
        criarAdmin(emailUnico(), "abc")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("AUT-008"));
        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("@spec:AC-581 Bloqueio grava situação e motivo, mesmo com carteira ativa")
    void bloqueioComCarteiraAtiva() throws Exception {
        Investidor investidor = investidor(SituacaoUsuario.ATIVO, "Com Carteira");
        Carteira carteira = carteira(investidor);

        bloquear(investidor.getId(), "uso abusivo")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("BLOQUEADO"))
                .andExpect(jsonPath("$.motivoBloqueio").value("uso abusivo"));

        assertThat(carteiraRepository.findById(carteira.getId()).orElseThrow().getAtiva()).isTrue();
        assertThat(usuarioRepository.findById(investidor.getId()).orElseThrow().getBloqueadoEm()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-582 Bloqueio sem motivo é recusado")
    void bloqueioSemMotivo() throws Exception {
        Usuario alvo = usuario(Papel.INVESTIDOR, SituacaoUsuario.ATIVO);

        for (String corpo : List.of("{}", json(Map.of("motivo", "   ")))) {
            mockMvc.perform(patch("/admin/usuarios/" + alvo.getId() + "/bloquear")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest());
        }
        assertThat(usuarioRepository.findById(alvo.getId()).orElseThrow().getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
    }

    @Test
    @DisplayName("@spec:AC-583 ADMIN não bloqueia a si mesmo")
    void adminNaoBloqueiaASiMesmo() throws Exception {
        bloquear(admin.getId(), "teste")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("AUT-011"));
        assertThat(usuarioRepository.findById(admin.getId()).orElseThrow().getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
    }

    @Test
    @DisplayName("@spec:AC-584 Bloqueado não entra; desbloqueado volta a entrar")
    void bloqueadoNaoEntraDesbloqueadoEntra() throws Exception {
        Usuario alvo = usuario(Papel.INVESTIDOR, SituacaoUsuario.ATIVO);
        bloquear(alvo.getId(), "teste").andExpect(status().isOk());

        login(alvo.getEmail()).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("AUT-009"));

        mockMvc.perform(patch("/admin/usuarios/" + alvo.getId() + "/desbloquear").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ATIVO"))
                .andExpect(jsonPath("$.motivoBloqueio").value(nullValue()));
        login(alvo.getEmail()).andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("@spec:AC-586 Usuário inexistente ou excluído não é encontrado")
    void inexistenteOuExcluido() throws Exception {
        Usuario excluido = usuario(Papel.INVESTIDOR, SituacaoUsuario.EXCLUIDO);
        for (Long id : List.of(Long.MAX_VALUE, excluido.getId())) {
            bloquear(id, "teste").andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("AUT-012"));
            mockMvc.perform(patch("/admin/usuarios/" + id + "/desbloquear").header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("AUT-012"));
        }
    }

    private ResultActions bloquear(Long id, String motivo) throws Exception {
        return mockMvc.perform(patch("/admin/usuarios/" + id + "/bloquear").header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("motivo", motivo))));
    }

    private ResultActions criarAdmin(String email, String senha) throws Exception {
        return mockMvc.perform(post("/admin/usuarios").header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email", email, "senha", senha))));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(new AuthLoginRequestDTO(email, SENHA))));
    }

    private List<JsonNode> listar(String situacao) throws Exception {
        String url = "/admin/usuarios?size=1000" + (situacao == null ? "" : "&situacao=" + situacao);
        String json = mockMvc.perform(get(url).header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<JsonNode> linhas = new ArrayList<>();
        objectMapper.readTree(json).get("content").forEach(linhas::add);
        return linhas;
    }

    private static JsonNode porId(List<JsonNode> linhas, Long id) {
        return linhas.stream().filter(l -> l.get("id").asLong() == id).findFirst().orElseThrow();
    }

    private static List<Long> ids(List<JsonNode> linhas) {
        return linhas.stream().map(l -> l.get("id").asLong()).toList();
    }

    private Usuario usuario(Papel papel, SituacaoUsuario situacao) {
        return usuarioRepository.save(new Usuario(null, emailUnico(), passwordEncoder.encode(SENHA), papel, situacao));
    }

    private Investidor investidor(SituacaoUsuario situacao, String nome) {
        // @MapsId: usuario e investidor precisam nascer na mesma transação, como no cadastro
        return transacao.execute(t -> {
            Investidor investidor = new Investidor();
            investidor.setUsuario(usuario(Papel.INVESTIDOR, situacao));
            investidor.setNome(nome);
            investidor.setCpf(String.format("%011d", System.nanoTime() % 100_000_000_000L));
            investidor.setCriadoEm(LocalDateTime.now());
            return investidorRepository.save(investidor);
        });
    }

    private Carteira carteira(Investidor investidor) {
        Corretora corretora = new Corretora();
        corretora.setCnpj(String.format("%014d", System.nanoTime() % 100_000_000_000_000L));
        corretora.setRazaoSocial("Corretora Teste");
        corretora = corretoraRepository.save(corretora);

        Carteira carteira = new Carteira();
        carteira.setInvestidor(investidor);
        carteira.setCorretora(corretora);
        carteira.setMercado(Mercado.BR);
        carteira.setNome("Carteira " + System.nanoTime());
        return carteiraRepository.save(carteira);
    }

    private void posicao(Carteira carteira, String ticker) {
        Acao acao = new Acao();
        acao.setTicker(ticker);
        acao.setMercado(Mercado.BR);
        acao.setMoeda("BRL");
        acao = acaoRepository.save(acao);

        CarteiraAcao posicao = new CarteiraAcao();
        posicao.setCarteira(carteira);
        posicao.setAcao(acao);
        posicao.setQuantidade(10);
        posicao.setPrecoMedio(new BigDecimal("12.34"));
        carteiraAcaoRepository.save(posicao);
    }

    private String token(Usuario usuario) {
        return jwtService.gerar(usuario.getId(), usuario.getPapel()).getTokenValue();
    }

    private String json(Object valor) throws Exception {
        return objectMapper.writeValueAsString(valor);
    }

    private static String emailUnico() {
        return "gestao" + System.nanoTime() + "@email.com";
    }
}
