package com.apiexternabackend.config;

import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.RegistroUso;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.CarteiraRepository;
import com.apiexternabackend.repositories.CorretoraRepository;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.repositories.RegistroUsoRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-28: registro de uso da API com SecurityFilterChain, JWT e banco reais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistroUsoIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private InvestidorRepository investidorRepository;
    @Autowired private CorretoraRepository corretoraRepository;
    @Autowired private CarteiraRepository carteiraRepository;
    @Autowired private RegistroUsoRepository registroRepository;
    @Autowired private TransactionTemplate transacao;

    private Investidor investidor;
    private String token;

    @BeforeEach
    void preparar() {
        investidor = investidor();
        token = jwtService.gerar(investidor.getUsuario().getId(), Papel.INVESTIDOR).getTokenValue();
    }

    @Test
    @DisplayName("@spec:AC-651 requisição autenticada deixa registro de quem, quando e o quê")
    void requisicaoAutenticadaRegistra() throws Exception {
        Carteira carteira = carteira(investidor);
        LocalDateTime antes = LocalDateTime.now();

        mockMvc.perform(get("/carteiras/{id}/posicoes", carteira.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        RegistroUso registro = esperarRegistro(investidor.getUsuario().getId());
        assertThat(registro.getMetodo()).isEqualTo("GET");
        assertThat(registro.getRota()).isEqualTo("/carteiras/{id}/posicoes");
        assertThat(registro.getStatus()).isEqualTo(200);
        assertThat(registro.getRegistradoEm()).isBetween(antes, LocalDateTime.now());
    }

    // a gravação é assíncrona: espera até 3 s pelo registro do usuário
    private RegistroUso esperarRegistro(Long usuarioId) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            List<RegistroUso> registros = registroRepository.findByUsuarioId(usuarioId);
            if (!registros.isEmpty()) {
                assertThat(registros).hasSize(1);
                return registros.get(0);
            }
            Thread.sleep(50);
        }
        throw new AssertionError("nenhum registro de uso para o usuário " + usuarioId);
    }

    private Usuario usuario(Papel papel) {
        return usuarioRepository.save(new Usuario(null, "uso" + System.nanoTime() + "@email.com", "hash", papel, SituacaoUsuario.ATIVO));
    }

    private Investidor investidor() {
        return transacao.execute(t -> {
            Investidor novo = new Investidor();
            novo.setUsuario(usuario(Papel.INVESTIDOR));
            novo.setNome("Investidor Uso");
            novo.setCpf(String.format("%011d", System.nanoTime() % 100_000_000_000L));
            novo.setCriadoEm(LocalDateTime.now());
            return investidorRepository.save(novo);
        });
    }

    private Carteira carteira(Investidor dono) {
        Corretora corretora = new Corretora();
        corretora.setCnpj(String.format("%014d", System.nanoTime() % 100_000_000_000_000L));
        corretora.setRazaoSocial("Corretora Uso");
        corretora = corretoraRepository.save(corretora);

        Carteira carteira = new Carteira();
        carteira.setInvestidor(dono);
        carteira.setCorretora(corretora);
        carteira.setMercado(Mercado.BR);
        carteira.setNome("Carteira " + System.nanoTime());
        return carteiraRepository.save(carteira);
    }
}
