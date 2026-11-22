package com.apiexternabackend.config;

import com.apiexternabackend.domains.RegistroUso;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.RegistroUsoRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-30: relatório de picos de requisições sobre o registro_uso da spec-28. Cada teste usa um dia
 * próprio no passado, para não contar registros gravados por outros testes do mesmo banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelatorioUsoIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RegistroUsoRepository registroRepository;

    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        tokenAdmin = jwtService.gerar(usuario(Papel.ADMIN).getId(), Papel.ADMIN).getTokenValue();
    }

    @Test
    @DisplayName("@spec:AC-660 Horas de pico do período")
    void horasDePico() throws Exception {
        LocalDate dia = LocalDate.now().minusDays(30);
        Long investidor = usuario(Papel.INVESTIDOR).getId();
        registrar(investidor, dia.atTime(14, 5), 3);
        registrar(investidor, dia.atTime(15, 10), 1);
        registrar(investidor, dia.atTime(9, 0), 2);

        JsonNode relatorio = relatorio("dataInicio=" + dia + "&dataFim=" + dia);

        assertThat(relatorio.get("total").asLong()).isEqualTo(6);
        assertThat(horas(relatorio)).containsExactly(dia.atTime(14, 0), dia.atTime(9, 0), dia.atTime(15, 0));
        assertThat(relatorio.get("picos")).extracting(p -> p.get("requisicoes").asLong()).containsExactly(3L, 2L, 1L);
    }

    @Test
    @DisplayName("@spec:AC-661 Usuários que mais chamaram a API, identificados")
    void usuariosIdentificados() throws Exception {
        LocalDate dia = LocalDate.now().minusDays(31);
        Usuario maisAtivo = usuario(Papel.INVESTIDOR);
        Usuario menosAtivo = usuario(Papel.INVESTIDOR);
        registrar(menosAtivo.getId(), dia.atTime(10, 0), 1);
        registrar(maisAtivo.getId(), dia.atTime(11, 0), 3);

        JsonNode relatorio = relatorio("dataInicio=" + dia + "&dataFim=" + dia);

        List<String> chaves = new ArrayList<>();
        relatorio.fieldNames().forEachRemaining(chaves::add);
        assertThat(chaves).containsExactlyInAnyOrder("total", "picos", "usuarios");
        JsonNode usuarios = relatorio.get("usuarios");
        assertThat(usuarios).extracting(u -> u.get("id").asLong()).containsExactly(maisAtivo.getId(), menosAtivo.getId());
        assertThat(usuarios).extracting(u -> u.get("email").asText()).containsExactly(maisAtivo.getEmail(), menosAtivo.getEmail());
        assertThat(usuarios).extracting(u -> u.get("requisicoes").asLong()).containsExactly(3L, 1L);
    }

    private JsonNode relatorio(String filtros) throws Exception {
        String json = mockMvc.perform(get("/admin/uso?" + filtros).header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private static List<LocalDateTime> horas(JsonNode relatorio) {
        List<LocalDateTime> horas = new ArrayList<>();
        relatorio.get("picos").forEach(p -> horas.add(LocalDateTime.parse(p.get("hora").asText())));
        return horas;
    }

    // grava direto no repositório: o relatório lê o registro, não importa como ele foi feito
    private void registrar(Long usuarioId, LocalDateTime quando, int vezes) {
        for (int i = 0; i < vezes; i++) {
            registroRepository.save(new RegistroUso(usuarioId, "GET", "/operacoes", 200, quando.plusSeconds(i)));
        }
    }

    private Usuario usuario(Papel papel) {
        return usuarioRepository.save(new Usuario(null, "relatorio" + System.nanoTime() + "@email.com", "hash", papel, SituacaoUsuario.ATIVO));
    }
}
