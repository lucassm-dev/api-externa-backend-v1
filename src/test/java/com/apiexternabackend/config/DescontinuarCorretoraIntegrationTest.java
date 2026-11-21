package com.apiexternabackend.config;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.RegistroAdministrativo;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.AcaoAdministrativa;
import com.apiexternabackend.domains.enums.AlvoAdministrativo;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.infra.adapter.CotacaoResultado;
import com.apiexternabackend.repositories.AcaoRepository;
import com.apiexternabackend.repositories.CarteiraRepository;
import com.apiexternabackend.repositories.CorretoraRepository;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.repositories.RegistroAdministrativoRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.apiexternabackend.services.CotacaoCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-20: descontinuar corretora com SecurityFilterChain, JWT e banco reais; só a cotação é simulada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DescontinuarCorretoraIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private InvestidorRepository investidorRepository;
    @Autowired private CorretoraRepository corretoraRepository;
    @Autowired private CarteiraRepository carteiraRepository;
    @Autowired private AcaoRepository acaoRepository;
    @Autowired private RegistroAdministrativoRepository registroRepository;
    @Autowired private TransactionTemplate transacao;
    @MockBean private CotacaoCacheService cotacaoCacheService;

    private Usuario admin;
    private String tokenAdmin;
    private Investidor investidor;
    private String tokenInvestidor;

    @BeforeEach
    void preparar() {
        admin = usuario(Papel.ADMIN);
        tokenAdmin = jwtService.gerar(admin.getId(), Papel.ADMIN).getTokenValue();
        investidor = investidor();
        tokenInvestidor = jwtService.gerar(investidor.getId(), Papel.INVESTIDOR).getTokenValue();
        when(cotacaoCacheService.obter(any(), anyBoolean()))
                .thenReturn(new CotacaoResultado(new BigDecimal("10.00"), LocalDateTime.now()));
    }

    @Test
    @DisplayName("@spec:AC-593 ADMIN vê quantas carteiras ativas serão afetadas")
    void contaCarteirasAfetadas() throws Exception {
        Corretora corretora = corretora();
        carteira(corretora);
        carteira(corretora);
        Carteira excluida = carteira(corretora);
        excluida.setAtiva(false);
        carteiraRepository.save(excluida);

        comAdmin(get("/corretoras/" + corretora.getId() + "/carteiras-afetadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carteirasAtivas").value(2));
    }

    @Test
    @DisplayName("@spec:AC-594 Descontinuar marca a corretora e tira da lista")
    void descontinuarMarcaETiraDaLista() throws Exception {
        Corretora corretora = corretora();
        carteira(corretora);

        descontinuar(corretora.getId(), "liquidação extrajudicial")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontinuada").value(true));

        assertThat(idsDaLista()).doesNotContain(corretora.getId());
        comAdmin(get("/corretoras/" + corretora.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontinuada").value(true));
    }

    @Test
    @DisplayName("@spec:AC-595 Descontinuação sem motivo ou repetida é recusada")
    void semMotivoOuRepetidaRecusada() throws Exception {
        Corretora ativa = corretora();
        Corretora jaDescontinuada = corretora();
        descontinuar(jaDescontinuada.getId(), "incorporação").andExpect(status().isOk());

        comAdmin(patch("/corretoras/" + ativa.getId() + "/descontinuar")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(corretoraRepository.findById(ativa.getId()).orElseThrow().getDescontinuada()).isFalse();

        descontinuar(jaDescontinuada.getId(), "de novo")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("COR-006"));
    }

    @Test
    @DisplayName("@spec:AC-596 Descontinuação fica no registro administrativo")
    void descontinuacaoRegistrada() throws Exception {
        Corretora corretora = corretora();
        long antes = registroRepository.count();

        descontinuar(corretora.getId(), "incorporação").andExpect(status().isOk());

        assertThat(registroRepository.count()).isEqualTo(antes + 1);
        RegistroAdministrativo registro = registroRepository.findByAlvoId(corretora.getId()).stream()
                .filter(r -> r.getAlvoTipo() == AlvoAdministrativo.CORRETORA).findFirst().orElseThrow();
        assertThat(registro.getAcao()).isEqualTo(AcaoAdministrativa.DESCONTINUACAO_CORRETORA);
        assertThat(registro.getAdminId()).isEqualTo(admin.getId());
        assertThat(registro.getMotivo()).isEqualTo("incorporação");
    }

    @Test
    @DisplayName("@spec:AC-597 Só o ADMIN descontinua e consulta carteiras afetadas")
    void soAdminUsaRotasNovas() throws Exception {
        Corretora corretora = corretora();
        String descontinuar = "/corretoras/" + corretora.getId() + "/descontinuar";
        String afetadas = "/corretoras/" + corretora.getId() + "/carteiras-afetadas";

        for (MockHttpServletRequestBuilder rota : List.of(get(afetadas), corpo(patch(descontinuar)))) {
            mockMvc.perform(rota.header("Authorization", "Bearer " + tokenInvestidor))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("AUT-007"));
        }
        for (MockHttpServletRequestBuilder rota : List.of(get(afetadas), corpo(patch(descontinuar)))) {
            mockMvc.perform(rota).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("AUT-005"));
        }
        comAdmin(get(afetadas)).andExpect(status().isOk());
        comAdmin(corpo(patch(descontinuar))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("@spec:AC-598 Criar carteira em corretora descontinuada é recusado")
    void criarCarteiraEmDescontinuadaRecusado() throws Exception {
        Corretora corretora = corretora();
        descontinuar(corretora.getId(), "liquidação").andExpect(status().isOk());
        long antes = carteiraRepository.count();

        criarCarteira(corretora.getId())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("COR-005"));
        assertThat(carteiraRepository.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("@spec:AC-599 Comprar e vender continuam funcionando")
    void comprarEVenderContinuam() throws Exception {
        Corretora corretora = corretora();
        Carteira carteira = carteira(corretora);
        String ticker = acao();
        descontinuar(corretora.getId(), "liquidação").andExpect(status().isOk());

        operar("compra", carteira.getId(), ticker, 10).andExpect(status().isCreated());
        operar("venda", carteira.getId(), ticker, 4).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("@spec:AC-600 Carteira mostra que a corretora foi descontinuada")
    void carteiraMostraSituacaoDaCorretora() throws Exception {
        Corretora descontinuada = corretora();
        Corretora normal = corretora();
        Carteira daDescontinuada = carteira(descontinuada);
        Carteira daNormal = carteira(normal);
        descontinuar(descontinuada.getId(), "liquidação").andExpect(status().isOk());

        String json = mockMvc.perform(get("/carteiras?size=100").header("Authorization", "Bearer " + tokenInvestidor))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<JsonNode> carteiras = new ArrayList<>();
        objectMapper.readTree(json).get("content").forEach(carteiras::add);

        assertThat(porId(carteiras, daDescontinuada.getId()).get("corretoraDescontinuada").asBoolean()).isTrue();
        assertThat(porId(carteiras, daNormal.getId()).get("corretoraDescontinuada").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-601 Corretora descontinuada com carteira ativa não é excluída")
    void descontinuadaComCarteiraNaoExclui() throws Exception {
        Corretora corretora = corretora();
        carteira(corretora);
        descontinuar(corretora.getId(), "liquidação").andExpect(status().isOk());

        comAdmin(delete("/corretoras/" + corretora.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("COR-004"));
        assertThat(corretoraRepository.findById(corretora.getId()).orElseThrow().getAtivo()).isTrue();
    }

    private ResultActions descontinuar(Long id, String motivo) throws Exception {
        return comAdmin(patch("/corretoras/" + id + "/descontinuar").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("motivo", motivo))));
    }

    private MockHttpServletRequestBuilder corpo(MockHttpServletRequestBuilder rota) {
        return rota.contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"teste\"}");
    }

    private ResultActions comAdmin(MockHttpServletRequestBuilder rota) throws Exception {
        return mockMvc.perform(rota.header("Authorization", "Bearer " + tokenAdmin));
    }

    private ResultActions criarCarteira(Long corretoraId) throws Exception {
        String corpo = "{\"corretoraId\":" + corretoraId + ",\"mercado\":\"BR\",\"nome\":\"Nova " + System.nanoTime() + "\"}";
        return mockMvc.perform(post("/carteiras").header("Authorization", "Bearer " + tokenInvestidor)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions operar(String tipo, Long carteiraId, String ticker, int quantidade) throws Exception {
        String corpo = "{\"carteiraId\":" + carteiraId + ",\"ticker\":\"" + ticker + "\",\"quantidade\":" + quantidade + "}";
        return mockMvc.perform(post("/operacoes/" + tipo).header("Authorization", "Bearer " + tokenInvestidor)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private List<Long> idsDaLista() throws Exception {
        String json = comAdmin(get("/corretoras?size=1000")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Long> ids = new ArrayList<>();
        objectMapper.readTree(json).get("content").forEach(c -> ids.add(c.get("id").asLong()));
        return ids;
    }

    private static JsonNode porId(List<JsonNode> linhas, Long id) {
        return linhas.stream().filter(l -> l.get("id").asLong() == id).findFirst().orElseThrow();
    }

    private Usuario usuario(Papel papel) {
        return usuarioRepository.save(new Usuario(null, "descont" + System.nanoTime() + "@email.com", "hash", papel, SituacaoUsuario.ATIVO));
    }

    private Investidor investidor() {
        return transacao.execute(t -> {
            Investidor novo = new Investidor();
            novo.setUsuario(usuario(Papel.INVESTIDOR));
            novo.setNome("Investidor Descontinuada");
            novo.setCpf(String.format("%011d", System.nanoTime() % 100_000_000_000L));
            novo.setCriadoEm(LocalDateTime.now());
            return investidorRepository.save(novo);
        });
    }

    private Corretora corretora() {
        Corretora corretora = new Corretora();
        corretora.setCnpj(String.format("%014d", System.nanoTime() % 100_000_000_000_000L));
        corretora.setRazaoSocial("Corretora Descontinuada " + System.nanoTime());
        return corretoraRepository.save(corretora);
    }

    private Carteira carteira(Corretora corretora) {
        Carteira carteira = new Carteira();
        carteira.setInvestidor(investidor);
        carteira.setCorretora(corretora);
        carteira.setMercado(Mercado.BR);
        carteira.setNome("Carteira " + System.nanoTime());
        return carteiraRepository.save(carteira);
    }

    private String acao() {
        Acao acao = new Acao();
        acao.setTicker("DSC" + (System.nanoTime() % 100000));
        acao.setMercado(Mercado.BR);
        acao.setMoeda("BRL");
        return acaoRepository.save(acao).getTicker();
    }
}
