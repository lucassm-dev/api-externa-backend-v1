package com.apiexternabackend.config;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.domains.enums.TipoOperacao;
import com.apiexternabackend.repositories.AcaoRepository;
import com.apiexternabackend.repositories.CarteiraRepository;
import com.apiexternabackend.repositories.CorretoraRepository;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.repositories.OperacaoRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-26 (#32) e spec-27 (#33): filtros do extrato, conforme ADR-018.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExtratoFiltrosIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private InvestidorRepository investidorRepository;
    @Autowired private CorretoraRepository corretoraRepository;
    @Autowired private CarteiraRepository carteiraRepository;
    @Autowired private AcaoRepository acaoRepository;
    @Autowired private OperacaoRepository operacaoRepository;
    @Autowired private TransactionTemplate transacao;

    private Investidor investidor;
    private String token;
    private Carteira carteira;
    private Acao acaoA;
    private Acao acaoB;

    @BeforeEach
    void preparar() {
        investidor = investidor();
        token = jwtService.gerar(investidor.getId(), Papel.INVESTIDOR).getTokenValue();
        carteira = carteira(investidor);
        acaoA = acao();
        acaoB = acao();
    }

    @Test
    @DisplayName("@spec:AC-640 Filtro só devolve operações do próprio investidor")
    void soDoProprioInvestidor() throws Exception {
        Long minha = operacao(carteira, acaoA, "2026-03-10T10:00").getId();
        Long alheia = operacao(carteira(investidor()), acaoA, "2026-03-10T11:00").getId();

        List<Long> ids = ids(extrato("?ticker=" + acaoA.getTicker() + "&dataInicio=2026-03-01&dataFim=2026-03-31"));

        assertThat(ids).containsExactly(minha).doesNotContain(alheia);
    }

    @Test
    @DisplayName("@spec:AC-641 Data final antes da inicial é recusada com OPE-006")
    void periodoInvertido() throws Exception {
        extrato("?dataInicio=2026-03-10&dataFim=2026-03-09")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("OPE-006"));
    }

    @Test
    @DisplayName("@spec:AC-642 Período inclui os dias das duas pontas")
    void periodoInclusivo() throws Exception {
        operacao(carteira, acaoA, "2026-02-28T23:59");
        Long inicio = operacao(carteira, acaoA, "2026-03-01T00:00").getId();
        Long fim = operacao(carteira, acaoA, "2026-03-31T23:59").getId();
        operacao(carteira, acaoA, "2026-04-01T00:00");

        assertThat(ids(extrato("?dataInicio=2026-03-01&dataFim=2026-03-31"))).containsExactly(fim, inicio);
        assertThat(ids(extrato("?dataInicio=2026-03-31"))).hasSize(2).contains(fim);
        assertThat(ids(extrato("?dataFim=2026-03-01"))).hasSize(2).contains(inicio);
    }

    @Test
    @DisplayName("@spec:AC-643 Período e ticker combinados devolvem a interseção, paginada")
    void filtrosCombinados() throws Exception {
        Long a1 = operacao(carteira, acaoA, "2026-03-05T10:00").getId();
        Long a2 = operacao(carteira, acaoA, "2026-03-06T10:00").getId();
        operacao(carteira, acaoA, "2026-05-01T10:00");
        operacao(carteira, acaoB, "2026-03-05T10:00");

        String filtro = "?ticker=" + acaoA.getTicker().toLowerCase() + "&dataInicio=2026-03-01&dataFim=2026-03-31";
        assertThat(ids(extrato(filtro))).containsExactly(a2, a1);
        extrato(filtro + "&size=1")
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));
        assertThat(ids(extrato(""))).hasSize(4);
    }

    @Test
    @DisplayName("@spec:AC-644 Filtro por carteira traz só as operações dela")
    void porCarteira() throws Exception {
        Carteira outra = carteira(investidor);
        Long nessa = operacao(carteira, acaoA, "2026-03-05T10:00").getId();
        operacao(outra, acaoA, "2026-03-06T10:00");

        assertThat(ids(extrato("?carteiraId=" + carteira.getId()))).containsExactly(nessa);
    }

    @Test
    @DisplayName("@spec:AC-645 Carteira de outro investidor devolve lista vazia")
    void carteiraAlheia() throws Exception {
        Carteira alheia = carteira(investidor());
        operacao(alheia, acaoA, "2026-03-05T10:00");
        operacao(carteira, acaoA, "2026-03-05T10:00");

        assertThat(ids(extrato("?carteiraId=" + alheia.getId()))).isEmpty();
    }

    @Test
    @DisplayName("@spec:AC-646 Filtro por tipo traz só as vendas")
    void porTipo() throws Exception {
        operacao(carteira, acaoA, "2026-03-05T10:00");
        Long v = venda(carteira, "2026-03-06T10:00", "5.00").getId();

        assertThat(ids(extrato("?tipo=VENDA"))).containsExactly(v);
    }

    @Test
    @DisplayName("@spec:AC-647 Resultado LUCRO traz só vendas com lucro realizado positivo")
    void resultadoLucro() throws Exception {
        operacao(carteira, acaoA, "2026-03-01T10:00");
        Long lucro = venda(carteira, "2026-03-02T10:00", "5.00").getId();
        venda(carteira, "2026-03-03T10:00", "-3.00");
        venda(carteira, "2026-03-04T10:00", "0.00");

        String json = extrato("?resultado=LUCRO").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(ids(extrato("?resultado=LUCRO"))).containsExactly(lucro);
        assertThat(objectMapper.readTree(json).at("/content/0/lucroRealizado").decimalValue())
                .isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("@spec:AC-648 Resultado PREJUIZO traz só vendas com lucro realizado negativo")
    void resultadoPrejuizo() throws Exception {
        operacao(carteira, acaoA, "2026-03-01T10:00");
        venda(carteira, "2026-03-02T10:00", "5.00");
        Long prejuizo = venda(carteira, "2026-03-03T10:00", "-3.00").getId();
        venda(carteira, "2026-03-04T10:00", "0.00");

        assertThat(ids(extrato("?resultado=PREJUIZO"))).containsExactly(prejuizo);
    }

    @Test
    @DisplayName("@spec:AC-649 Resultado só traz vendas do próprio investidor")
    void resultadoSoDoProprio() throws Exception {
        Long minha = venda(carteira, "2026-03-02T10:00", "5.00").getId();
        Long alheia = venda(carteira(investidor()), "2026-03-02T11:00", "7.00").getId();

        assertThat(ids(extrato("?resultado=LUCRO"))).containsExactly(minha).doesNotContain(alheia);
    }

    @Test
    @DisplayName("@spec:AC-650 Valor inválido em parâmetro de URL responde 400 VAL-002")
    void parametroInvalido() throws Exception {
        extrato("?tipo=TROCA")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VAL-002"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("tipo"));
    }

    private Operacao venda(Carteira carteira, String dataHora, String lucroRealizado) {
        Operacao op = new Operacao();
        op.setCarteira(carteira);
        op.setAcao(acaoA);
        op.setTipo(TipoOperacao.VENDA);
        op.setQuantidade(1);
        op.setPrecoUnitario(new BigDecimal("10.00"));
        op.setDataHora(LocalDateTime.parse(dataHora));
        op.setLucroRealizado(new BigDecimal(lucroRealizado));
        op.setLucroRealizadoBrl(new BigDecimal(lucroRealizado));
        return operacaoRepository.save(op);
    }

    private ResultActions extrato(String query) throws Exception {
        return mockMvc.perform(get("/operacoes" + query).header("Authorization", "Bearer " + token));
    }

    private List<Long> ids(ResultActions resposta) throws Exception {
        String json = resposta.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Long> ids = new ArrayList<>();
        objectMapper.readTree(json).get("content").forEach(o -> ids.add(o.get("id").asLong()));
        return ids;
    }

    private Operacao operacao(Carteira carteira, Acao acao, String dataHora) {
        Operacao op = new Operacao();
        op.setCarteira(carteira);
        op.setAcao(acao);
        op.setTipo(TipoOperacao.COMPRA);
        op.setQuantidade(1);
        op.setPrecoUnitario(new BigDecimal("10.00"));
        op.setDataHora(LocalDateTime.parse(dataHora));
        return operacaoRepository.save(op);
    }

    private Investidor investidor() {
        return transacao.execute(t -> {
            Investidor novo = new Investidor();
            novo.setUsuario(usuarioRepository.save(new Usuario(null, "extrato" + System.nanoTime() + "@email.com",
                    "hash", Papel.INVESTIDOR, SituacaoUsuario.ATIVO)));
            novo.setNome("Investidor Extrato");
            novo.setCpf(String.format("%011d", System.nanoTime() % 100_000_000_000L));
            novo.setCriadoEm(LocalDateTime.now());
            return investidorRepository.save(novo);
        });
    }

    private Carteira carteira(Investidor dono) {
        Corretora corretora = new Corretora();
        corretora.setCnpj(String.format("%014d", System.nanoTime() % 100_000_000_000_000L));
        corretora.setRazaoSocial("Corretora Extrato " + System.nanoTime());
        Carteira nova = new Carteira();
        nova.setInvestidor(dono);
        nova.setCorretora(corretoraRepository.save(corretora));
        nova.setMercado(Mercado.BR);
        nova.setNome("Carteira " + System.nanoTime());
        return carteiraRepository.save(nova);
    }

    private Acao acao() {
        Acao acao = new Acao();
        acao.setTicker("EXT" + (System.nanoTime() % 100000));
        acao.setMercado(Mercado.BR);
        acao.setMoeda("BRL");
        return acaoRepository.save(acao);
    }
}
