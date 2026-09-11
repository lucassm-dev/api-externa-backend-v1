package com.apiexternabackend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A lista de origens vem por propriedade, e não do application-test.properties: é isso
 * que prova o AC-515 — ORIGEM_CONFIGURADA só é aceita porque foi configurada aqui.
 */
@SpringBootTest(properties =
        "cors.allowed-origins=http://localhost:4200,http://origem-de-teste.local")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsIntegrationTest {

    private static final String ORIGEM_PERMITIDA = "http://localhost:4200";
    private static final String ORIGEM_CONFIGURADA = "http://origem-de-teste.local";
    private static final String ORIGEM_DESCONHECIDA = "http://origem-nao-listada.local";

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("@spec:AC-510 Consulta prévia do navegador passa sem token em rota autenticada")
    void devePermitirPreflightSemToken() throws Exception {
        mockMvc.perform(options("/carteiras")
                        .header("Origin", ORIGEM_PERMITIDA)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_PERMITIDA));
    }

    @Test
    @DisplayName("@spec:AC-511 Resposta autoriza explicitamente a origem que chamou")
    void deveEcoarOrigemPermitida() throws Exception {
        mockMvc.perform(get("/carteiras").header("Origin", ORIGEM_PERMITIDA))
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_PERMITIDA));
    }

    @Test
    @DisplayName("@spec:AC-512 Origem não listada não recebe cabeçalho de permissão")
    void deveRecusarOrigemNaoListada() throws Exception {
        mockMvc.perform(options("/carteiras")
                        .header("Origin", ORIGEM_DESCONHECIDA)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        mockMvc.perform(get("/carteiras").header("Origin", ORIGEM_DESCONHECIDA))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("@spec:AC-513 PATCH está entre os métodos permitidos (renomear carteira)")
    void devePermitirPatch() throws Exception {
        mockMvc.perform(options("/carteiras/1")
                        .header("Origin", ORIGEM_PERMITIDA)
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("PATCH"))));
    }

    @Test
    @DisplayName("@spec:AC-514 Origem permitida não abre rota protegida — segue 401 AUT-005")
    void naoDeveAbrirRotaProtegida() throws Exception {
        mockMvc.perform(get("/carteiras").header("Origin", ORIGEM_PERMITIDA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-005"));
    }

    @Test
    @DisplayName("@spec:AC-515 A origem aceita vem da configuração, não do código")
    void deveHonrarOrigemVindaDaConfiguracao() throws Exception {
        mockMvc.perform(options("/carteiras")
                        .header("Origin", ORIGEM_CONFIGURADA)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_CONFIGURADA));
    }

    @Test
    @DisplayName("@spec:AC-516 A API não pede credenciais entre origens")
    void naoDevePedirCredenciais() throws Exception {
        mockMvc.perform(options("/carteiras")
                        .header("Origin", ORIGEM_PERMITIDA)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));

        mockMvc.perform(get("/carteiras").header("Origin", ORIGEM_PERMITIDA))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
}
