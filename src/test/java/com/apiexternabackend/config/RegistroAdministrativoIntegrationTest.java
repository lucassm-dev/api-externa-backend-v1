package com.apiexternabackend.config;

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
import com.apiexternabackend.repositories.CarteiraRepository;
import com.apiexternabackend.repositories.CorretoraRepository;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.repositories.RegistroAdministrativoRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-19: registro das ações administrativas com SecurityFilterChain, JWT e banco reais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistroAdministrativoIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private InvestidorRepository investidorRepository;
    @Autowired private CorretoraRepository corretoraRepository;
    @Autowired private CarteiraRepository carteiraRepository;
    @Autowired private TransactionTemplate transacao;
    @SpyBean private RegistroAdministrativoRepository registroRepository;

    private Usuario admin;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        admin = usuario(Papel.ADMIN);
        tokenAdmin = jwtService.gerar(admin.getId(), Papel.ADMIN).getTokenValue();
    }

    @AfterEach
    void restaurarSpy() {
        Mockito.reset(registroRepository);
    }

    @Test
    @DisplayName("@spec:AC-587 Bloqueio e desbloqueio geram um registro cada")
    void bloqueioEDesbloqueioRegistram() throws Exception {
        Usuario alvo = usuario(Papel.INVESTIDOR);
        long antes = registroRepository.count();

        bloquear(alvo.getId(), "uso abusivo").andExpect(status().isOk());
        desbloquear(alvo.getId()).andExpect(status().isOk());

        assertThat(registroRepository.count()).isEqualTo(antes + 2);
        List<RegistroAdministrativo> registros = registroRepository.findByAlvoId(alvo.getId()).stream()
                .filter(r -> r.getAlvoTipo() == AlvoAdministrativo.USUARIO).toList();
        assertThat(registros).hasSize(2);
        RegistroAdministrativo bloqueio = registros.stream().filter(r -> r.getAcao() == AcaoAdministrativa.BLOQUEIO).findFirst().orElseThrow();
        RegistroAdministrativo desbloqueio = registros.stream().filter(r -> r.getAcao() == AcaoAdministrativa.DESBLOQUEIO).findFirst().orElseThrow();
        assertThat(bloqueio.getAdminId()).isEqualTo(admin.getId());
        assertThat(bloqueio.getMotivo()).isEqualTo("uso abusivo");
        assertThat(bloqueio.getRegistradoEm()).isNotNull();
        assertThat(desbloqueio.getAdminId()).isEqualTo(admin.getId());
        assertThat(desbloqueio.getMotivo()).isNull();
        assertThat(desbloqueio.getRegistradoEm()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-588 Exclusão de corretora gera um registro")
    void exclusaoDeCorretoraRegistra() throws Exception {
        Corretora corretora = corretora();
        long antes = registroRepository.count();

        excluirCorretora(corretora.getId()).andExpect(status().isNoContent());

        assertThat(registroRepository.count()).isEqualTo(antes + 1);
        RegistroAdministrativo registro = registroRepository.findByAlvoId(corretora.getId()).stream()
                .filter(r -> r.getAlvoTipo() == AlvoAdministrativo.CORRETORA).findFirst().orElseThrow();
        assertThat(registro.getAcao()).isEqualTo(AcaoAdministrativa.EXCLUSAO_CORRETORA);
        assertThat(registro.getAdminId()).isEqualTo(admin.getId());
        assertThat(registro.getMotivo()).isNull();
        assertThat(registro.getRegistradoEm()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-589 Ação recusada não gera registro")
    void acaoRecusadaNaoRegistra() throws Exception {
        Corretora comCarteira = corretora();
        carteira(investidor(), comCarteira);
        Usuario alvo = usuario(Papel.INVESTIDOR);
        long antes = registroRepository.count();

        bloquear(admin.getId(), "teste").andExpect(status().isUnprocessableEntity());
        mockMvc.perform(patch("/admin/usuarios/" + alvo.getId() + "/bloquear").header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        bloquear(Long.MAX_VALUE, "teste").andExpect(status().isNotFound());
        excluirCorretora(comCarteira.getId()).andExpect(status().isUnprocessableEntity());

        assertThat(registroRepository.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("@spec:AC-590 Falha ao gravar o registro desfaz a ação")
    void falhaNoRegistroDesfazAcao() throws Exception {
        Usuario alvo = usuario(Papel.INVESTIDOR);
        Mockito.doThrow(new IllegalStateException("falha simulada")).when(registroRepository).save(any());

        int status = bloquear(alvo.getId(), "uso abusivo").andReturn().getResponse().getStatus();

        assertThat(status).isGreaterThanOrEqualTo(500);
        Usuario depois = usuarioRepository.findById(alvo.getId()).orElseThrow();
        assertThat(depois.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(depois.getMotivoBloqueio()).isNull();
    }

    @Test
    @DisplayName("@spec:AC-591 A API não altera nem exclui registros")
    void apiNaoAlteraNemExcluiRegistros() throws IOException {
        try (Stream<Path> arquivos = Files.walk(Path.of("src/main/java/com/apiexternabackend/resources"))) {
            List<Path> usamRegistro = arquivos.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> ler(p).contains("RegistroAdministrativo")).toList();
            assertThat(usamRegistro).isEmpty();
        }
        assertThat(CrudRepository.class.isAssignableFrom(RegistroAdministrativoRepository.class)).isFalse();
        assertThat(Arrays.stream(RegistroAdministrativoRepository.class.getMethods()).map(Method::getName))
                .noneMatch(nome -> nome.startsWith("delete") || nome.startsWith("update") || nome.startsWith("saveAll"));
    }

    @Test
    @DisplayName("@spec:AC-592 Registro não guarda senha nem token")
    void registroSemSenhaNemToken() {
        assertThat(Arrays.stream(RegistroAdministrativo.class.getDeclaredFields()).map(Field::getName).map(String::toLowerCase))
                .noneMatch(nome -> nome.contains("senha") || nome.contains("hash") || nome.contains("token"));
        assertThat(ler(Path.of("src/main/resources/db/migration/V19__registro_administrativo.sql")).toLowerCase())
                .doesNotContain("senha").doesNotContain("hash").doesNotContain("token");
    }

    private ResultActions bloquear(Long id, String motivo) throws Exception {
        return mockMvc.perform(patch("/admin/usuarios/" + id + "/bloquear").header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"" + motivo + "\"}"));
    }

    private ResultActions desbloquear(Long id) throws Exception {
        return mockMvc.perform(patch("/admin/usuarios/" + id + "/desbloquear").header("Authorization", "Bearer " + tokenAdmin));
    }

    private ResultActions excluirCorretora(Long id) throws Exception {
        return mockMvc.perform(delete("/corretoras/" + id).header("Authorization", "Bearer " + tokenAdmin));
    }

    private Usuario usuario(Papel papel) {
        return usuarioRepository.save(new Usuario(null, "registro" + System.nanoTime() + "@email.com", "hash", papel, SituacaoUsuario.ATIVO));
    }

    private Investidor investidor() {
        return transacao.execute(t -> {
            Investidor investidor = new Investidor();
            investidor.setUsuario(usuario(Papel.INVESTIDOR));
            investidor.setNome("Investidor Registro");
            investidor.setCpf(String.format("%011d", System.nanoTime() % 100_000_000_000L));
            investidor.setCriadoEm(LocalDateTime.now());
            return investidorRepository.save(investidor);
        });
    }

    private Corretora corretora() {
        Corretora corretora = new Corretora();
        corretora.setCnpj(String.format("%014d", System.nanoTime() % 100_000_000_000_000L));
        corretora.setRazaoSocial("Corretora Registro");
        return corretoraRepository.save(corretora);
    }

    private void carteira(Investidor investidor, Corretora corretora) {
        Carteira carteira = new Carteira();
        carteira.setInvestidor(investidor);
        carteira.setCorretora(corretora);
        carteira.setMercado(Mercado.BR);
        carteira.setNome("Carteira " + System.nanoTime());
        carteiraRepository.save(carteira);
    }

    private static String ler(Path arquivo) {
        try {
            return Files.readString(arquivo);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
