package com.apiexternabackend.config;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.infra.adapter.BrapiAdapter;
import com.apiexternabackend.infra.adapter.CotacaoResultado;
import com.apiexternabackend.repositories.AcaoRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-21: a ação entra no catálogo pela pesquisa, com JWT e banco reais; só a fonte BR é simulada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcaoPelaPesquisaIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AcaoRepository acaoRepository;
    @MockBean private BrapiAdapter brapiAdapter;

    // investidor sem carteira nenhuma: AC-605 vale para todos os cenários
    private String token;

    @BeforeEach
    void preparar() {
        Usuario investidor = usuarioRepository.save(new Usuario(null, "pesquisa" + System.nanoTime() + "@email.com",
                "hash", Papel.INVESTIDOR, SituacaoUsuario.ATIVO));
        token = jwtService.gerar(investidor.getId(), Papel.INVESTIDOR).getTokenValue();
        when(brapiAdapter.buscarCotacao(any()))
                .thenReturn(new CotacaoResultado(new BigDecimal("12.34"), LocalDateTime.now()));
    }

    @Test
    @DisplayName("@spec:AC-602 Ticker novo e válido entra no catálogo na primeira pesquisa")
    void tickerNovoEntraNoCatalogo() throws Exception {
        String ticker = tickerNovo();

        pesquisar(ticker, "BR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value(ticker))
                .andExpect(jsonPath("$.mercado").value("BR"))
                .andExpect(jsonPath("$.moeda").value("BRL"))
                .andExpect(jsonPath("$.cotacaoAtual").value(12.34))
                .andExpect(jsonPath("$.dataHoraCotacao").isNotEmpty());

        assertThat(acaoRepository.findByTickerAndAtivoTrue(ticker)).isPresent();
    }

    @Test
    @DisplayName("@spec:AC-603 Segunda pesquisa reaproveita o catálogo")
    void segundaPesquisaReaproveita() throws Exception {
        String ticker = tickerNovo();
        Long id = idDe(pesquisar(ticker, "BR").andExpect(status().isOk()));

        pesquisar(ticker, "BR").andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        pesquisar(ticker, null).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));

        verify(brapiAdapter, times(1)).buscarCotacao(ticker);
        assertThat(acaoRepository.findAll()).filteredOn(a -> a.getTicker().equals(ticker) && a.getAtivo()).hasSize(1);
    }

    @Test
    @DisplayName("@spec:AC-604 Ticker inexistente na fonte não entra")
    void tickerInexistenteNaoEntra() throws Exception {
        String ticker = tickerNovo();
        when(brapiAdapter.buscarCotacao(ticker))
                .thenThrow(new RegraVioladaException("EXT-008", "Ticker não encontrado na fonte BR: " + ticker));

        pesquisar(ticker, "BR")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("EXT-008"));

        assertThat(acaoRepository.findAll()).noneMatch(a -> a.getTicker().equals(ticker));
    }

    @Test
    @DisplayName("@spec:AC-605 Investidor sem carteira consegue pesquisar")
    void investidorSemCarteiraPesquisa() throws Exception {
        String ticker = tickerNovo();

        pesquisar(ticker, "BR").andExpect(status().isOk());

        assertThat(acaoRepository.findByTickerAndAtivoTrue(ticker)).isPresent();
    }

    @Test
    @DisplayName("@spec:AC-606 Ticker fora do catálogo sem mercado não consulta a fonte")
    void semMercadoNaoConsultaFonte() throws Exception {
        String ticker = tickerNovo();

        pesquisar(ticker, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ACA-001"));

        verify(brapiAdapter, never()).buscarCotacao(ticker);
        assertThat(acaoRepository.findAll()).noneMatch(a -> a.getTicker().equals(ticker));
    }

    @Test
    @DisplayName("@spec:AC-607 Não existe mais cadastrar nem excluir ação")
    void semCadastroNemExclusao() throws Exception {
        Acao acao = new Acao();
        acao.setTicker(tickerNovo());
        acao.setMercado(Mercado.BR);
        acao.setMoeda("BRL");
        acao = acaoRepository.save(acao);

        int cadastro = mockMvc.perform(post("/acoes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ticker\":\"NOVA3\",\"mercado\":\"BR\"}"))
                .andReturn().getResponse().getStatus();
        int exclusao = mockMvc.perform(delete("/acoes/" + acao.getTicker()).header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();

        assertThat(cadastro).isBetween(400, 499);
        assertThat(exclusao).isBetween(400, 499);
        assertThat(acaoRepository.findById(acao.getId()).orElseThrow().getAtivo()).isTrue();
        assertThat(acaoRepository.findAll()).noneMatch(a -> a.getTicker().equals("NOVA3"));
    }

    @Test
    @DisplayName("@spec:AC-608 Catálogo de erros sem as regras removidas")
    void catalogoDeErrosSemRegrasRemovidas() throws IOException {
        String erros = Files.readString(Path.of("docs/erros.md"));

        assertThat(erros).doesNotContain("| ACA-002 |").doesNotContain("| ACA-003 |").doesNotContain("| ACA-004 |");
    }

    private ResultActions pesquisar(String ticker, String mercado) throws Exception {
        var requisicao = get("/acoes/ticker/" + ticker).header("Authorization", "Bearer " + token);
        if (mercado != null) {
            requisicao.param("mercado", mercado);
        }
        return mockMvc.perform(requisicao);
    }

    private static Long idDe(ResultActions resultado) throws Exception {
        String json = resultado.andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asLong();
    }

    private static String tickerNovo() {
        return "PQ" + (System.nanoTime() % 1_000_000);
    }
}
