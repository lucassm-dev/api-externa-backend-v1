package com.apiexternabackend.config;

import com.apiexternabackend.domains.dtos.AcaoRequestDTO;
import com.apiexternabackend.domains.dtos.AcaoResponseDTO;
import com.apiexternabackend.domains.dtos.CarteiraRequestDTO;
import com.apiexternabackend.domains.dtos.CarteiraResponseDTO;
import com.apiexternabackend.domains.dtos.CorretoraRequestDTO;
import com.apiexternabackend.domains.dtos.CorretoraResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.apiexternabackend.services.AcaoService;
import com.apiexternabackend.services.BarraCotacoesService;
import com.apiexternabackend.services.CarteiraService;
import com.apiexternabackend.services.ConsultaOperacaoService;
import com.apiexternabackend.services.CorretoraService;
import com.apiexternabackend.services.InvestidorService;
import com.apiexternabackend.services.OperacaoService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-16: matriz de acesso com SecurityFilterChain, @PreAuthorize e tokens reais de cada papel.
 * Os services ficam mockados para isolar a regra de acesso das integrações externas (CVM, cotação).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatrizAcessoIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;

    @MockBean private CorretoraService corretoraService;
    @MockBean private AcaoService acaoService;
    @MockBean private BarraCotacoesService barraCotacoesService;
    @MockBean private CarteiraService carteiraService;
    @MockBean private OperacaoService operacaoService;
    @MockBean private ConsultaOperacaoService consultaOperacaoService;
    @MockBean private InvestidorService investidorService;

    private String tokenInvestidor;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        // spec-18: a checagem por requisição exige que o dono do token exista e esteja ativo
        tokenInvestidor = jwtService.gerar(usuario(Papel.INVESTIDOR), Papel.INVESTIDOR).getTokenValue();
        tokenAdmin = jwtService.gerar(usuario(Papel.ADMIN), Papel.ADMIN).getTokenValue();

        CorretoraResponseDTO corretora = new CorretoraResponseDTO();
        corretora.setId(10L);
        when(corretoraService.cadastrar(any())).thenReturn(corretora);

        AcaoResponseDTO acao = new AcaoResponseDTO();
        acao.setId(20L);
        when(acaoService.cadastrar(any(), anyLong())).thenReturn(acao);
        when(acaoService.atualizarCotacao(anyLong(), anyBoolean())).thenReturn(acao);

        CarteiraResponseDTO carteira = new CarteiraResponseDTO();
        carteira.setId(30L);
        when(carteiraService.criar(any(), anyLong())).thenReturn(carteira);
    }

    @Test
    @DisplayName("@spec:AC-559 Investidor e ADMIN consultam corretoras; sem token 401 AUT-005")
    void doisPapeisConsultamCorretoras() throws Exception {
        List<Supplier<MockHttpServletRequestBuilder>> rotas = List.of(
                () -> get("/corretoras"),
                () -> get("/corretoras/1"),
                () -> get("/corretoras/cnpj/02332886000104"));

        for (var rota : rotas) {
            mockMvc.perform(rota.get().header("Authorization", "Bearer " + tokenInvestidor)).andExpect(status().isOk());
            mockMvc.perform(rota.get().header("Authorization", "Bearer " + tokenAdmin)).andExpect(status().isOk());
            semToken(rota);
        }
    }

    @Test
    @DisplayName("@spec:AC-560 Só o ADMIN cadastra e exclui corretora; investidor 403 AUT-007; sem token 401 AUT-005")
    void soAdminAlteraCorretora() throws Exception {
        List<Supplier<MockHttpServletRequestBuilder>> rotas = List.of(
                () -> json(post("/corretoras"), new CorretoraRequestDTO("02332886000104")),
                () -> delete("/corretoras/1"));

        for (var rota : rotas) {
            atendido(rota, tokenAdmin);
            negado(rota, tokenInvestidor);
            semToken(rota);
        }
    }

    @Test
    @DisplayName("@spec:AC-561 Investidor e ADMIN pesquisam ações e a barra de cotações; sem token 401 AUT-005")
    void doisPapeisPesquisamAcoes() throws Exception {
        List<Supplier<MockHttpServletRequestBuilder>> rotas = List.of(
                () -> get("/acoes"),
                () -> get("/acoes/ticker/PETR4"),
                () -> get("/mercado/barra-cotacoes"));

        for (var rota : rotas) {
            mockMvc.perform(rota.get().header("Authorization", "Bearer " + tokenInvestidor)).andExpect(status().isOk());
            mockMvc.perform(rota.get().header("Authorization", "Bearer " + tokenAdmin)).andExpect(status().isOk());
            semToken(rota);
        }
    }

    @Test
    @DisplayName("@spec:AC-562 Investidor e ADMIN cadastram, atualizam cotação e excluem ação até a #24; sem token 401 AUT-005")
    void doisPapeisAlteramAcoes() throws Exception {
        List<Supplier<MockHttpServletRequestBuilder>> rotas = List.of(
                () -> json(post("/acoes"), new AcaoRequestDTO("PETR4", Mercado.BR)),
                () -> put("/acoes/20/atualizar-cotacao"),
                () -> delete("/acoes/PETR4"));

        for (var rota : rotas) {
            atendido(rota, tokenInvestidor);
            atendido(rota, tokenAdmin);
            semToken(rota);
        }
    }

    @Test
    @DisplayName("@spec:AC-563 ADMIN recebe 403 AUT-007 em carteiras, operações e perfil de investidor; investidor é atendido; sem token 401 AUT-005")
    void adminNaoAcessaDadoDoInvestidor() throws Exception {
        List<Supplier<MockHttpServletRequestBuilder>> rotas = List.of(
                () -> get("/carteiras"),
                () -> json(post("/carteiras"), new CarteiraRequestDTO(10L, Mercado.BR, "Carteira")),
                () -> json(patch("/carteiras/30"), java.util.Map.of("nome", "Nova")),
                () -> delete("/carteiras/30"),
                () -> get("/carteiras/30/posicoes"),
                () -> get("/carteiras/30/lucro-realizado"),
                () -> get("/carteiras/30/consolidado"),
                () -> get("/operacoes"),
                () -> delete("/operacoes/40"),
                () -> get("/investidores"),
                () -> get("/investidores/1"),
                () -> delete("/investidores/1"));

        for (var rota : rotas) {
            atendido(rota, tokenInvestidor);
            negado(rota, tokenAdmin);
            semToken(rota);
        }
    }

    private void atendido(Supplier<MockHttpServletRequestBuilder> rota, String token) throws Exception {
        mockMvc.perform(rota.get().header("Authorization", "Bearer " + token))
                .andExpect(r -> assertThat(r.getResponse().getStatus())
                        .as(r.getRequest().getMethod() + " " + r.getRequest().getRequestURI())
                        .isBetween(200, 299));
    }

    private void negado(Supplier<MockHttpServletRequestBuilder> rota, String token) throws Exception {
        mockMvc.perform(rota.get().header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AUT-007"));
    }

    private void semToken(Supplier<MockHttpServletRequestBuilder> rota) throws Exception {
        mockMvc.perform(rota.get())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-005"));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, Object corpo) {
        try {
            return req.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(corpo));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Long usuario(Papel papel) {
        return usuarioRepository.save(new Usuario(null, "matriz" + System.nanoTime() + "@email.com", "hash",
                papel, SituacaoUsuario.ATIVO)).getId();
    }
}
