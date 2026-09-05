package com.apiexternabackend.resources;

import com.apiexternabackend.config.InvestidorPrincipal;
import com.apiexternabackend.domains.dtos.BarraCotacoesResponseDTO;
import com.apiexternabackend.domains.dtos.ItemBarraCotacoesDTO;
import com.apiexternabackend.resources.exceptions.GlobalExceptionHandler;
import com.apiexternabackend.services.BarraCotacoesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MercadoResource.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MercadoResourceTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private BarraCotacoesService service;

    @BeforeEach
    void autenticarComoInvestidor() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new InvestidorPrincipal(1L, "joao@email.com"), null,
                List.of(new SimpleGrantedAuthority("ROLE_INVESTIDOR"))));
    }

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("@spec:AC-498 @spec:AC-502 GET /mercado/barra-cotacoes retorna os itens e o horário de atualização")
    void deveRetornarBarraDeCotacoes() throws Exception {
        BarraCotacoesResponseDTO resposta = new BarraCotacoesResponseDTO(
                List.of(new ItemBarraCotacoesDTO("PETR4", "Petrobras", new BigDecimal("47.50"), new BigDecimal("-1.31"), null)),
                LocalDateTime.now(), List.of());
        when(service.obter()).thenReturn(resposta);

        mockMvc.perform(get("/mercado/barra-cotacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].simbolo").value("PETR4"))
                .andExpect(jsonPath("$.atualizadoEm").isNotEmpty());
    }

    @Test
    @DisplayName("@spec:AC-499 GET /mercado/barra-cotacoes traz avisos quando uma fonte falhou")
    void deveTrazerAvisosQuandoFonteFalhou() throws Exception {
        BarraCotacoesResponseDTO resposta = new BarraCotacoesResponseDTO(
                List.of(), LocalDateTime.now(), List.of("Ações/índices indisponíveis no momento (brapi)."));
        when(service.obter()).thenReturn(resposta);

        mockMvc.perform(get("/mercado/barra-cotacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisos[0]").value(org.hamcrest.Matchers.containsString("brapi")));
    }
}