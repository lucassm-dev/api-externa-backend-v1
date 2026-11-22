package com.apiexternabackend.config;

import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.apiexternabackend.services.AutenticacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-15: acesso (usuario) separado do perfil (investidor), com banco e SecurityFilterChain reais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentidadeUsuarioIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private InvestidorRepository investidorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AutenticacaoService autenticacaoService;
    @Autowired private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("@spec:AC-553 Autocadastro cria usuário INVESTIDOR ATIVO e investidor com o mesmo id, senha em hash")
    void autocadastroCriaUsuarioEInvestidor() throws Exception {
        String email = emailUnico();
        Long id = cadastrar(email);

        Usuario usuario = usuarioRepository.findById(id).orElseThrow();
        assertThat(usuario.getEmail()).isEqualTo(email);
        assertThat(usuario.getPapel()).isEqualTo(Papel.INVESTIDOR);
        assertThat(usuario.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(usuario.getSenha()).isNotEqualTo("senha123");
        assertThat(passwordEncoder.matches("senha123", usuario.getSenha())).isTrue();
        assertThat(investidorRepository.findById(id)).isPresent();
    }

    @Test
    @DisplayName("@spec:AC-554 Falha ao gravar o investidor não deixa usuário gravado")
    void falhaNoInvestidorDesfazOUsuario() {
        String email = emailUnico();
        // nome nulo passa direto pelo service (a validação é no controller) e quebra o NOT NULL de investidor
        AuthCadastroRequestDTO semNome = new AuthCadastroRequestDTO(null, email, cpfUnico(), "senha123");

        assertThatThrownBy(() -> autenticacaoService.cadastrar(semNome)).isInstanceOf(RuntimeException.class);

        assertThat(usuarioRepository.existsByEmailAndSituacaoNot(email, SituacaoUsuario.EXCLUIDO)).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-552 Bloqueado: senha certa → 403 AUT-009; senha errada → 401 AUT-004 genérico")
    void bloqueadoSoEAvisadoComSenhaCerta() throws Exception {
        String email = emailUnico();
        Usuario usuario = usuarioRepository.findById(cadastrar(email)).orElseThrow();
        usuario.setSituacao(SituacaoUsuario.BLOQUEADO);
        usuarioRepository.save(usuario);

        login(email, "senha123")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AUT-009"))
                .andExpect(jsonPath("$.message").value("Conta bloqueada. Procure o suporte."));
        login(email, "senhaErrada1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-004"))
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("@spec:AC-555 Excluir a conta marca usuário EXCLUIDO, investidor inativo e recusa o login")
    void excluirContaEncerraOAcesso() throws Exception {
        String email = emailUnico();
        Long id = cadastrar(email);
        String token = objectMapper.readValue(login(email, "senha123").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), AuthTokenResponseDTO.class).getToken();

        mockMvc.perform(delete("/investidores/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(usuarioRepository.findById(id).orElseThrow().getSituacao()).isEqualTo(SituacaoUsuario.EXCLUIDO);
        assertThat(investidorRepository.findById(id).orElseThrow().getAtivo()).isFalse();
        login(email, "senha123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-004"));
    }

    @Test
    @DisplayName("@spec:AC-557 Consulta de investidores não traz administrador")
    void consultaDeInvestidoresNaoTrazAdmin() throws Exception {
        Usuario admin = usuarioRepository.save(new Usuario(null, emailUnico(), "hash", Papel.ADMIN, SituacaoUsuario.ATIVO));
        Long investidorId = cadastrar(emailUnico());

        assertThat(investidorRepository.findAll()).extracting(Investidor::getId)
                .contains(investidorId)
                .doesNotContain(admin.getId());
    }

    @Test
    @DisplayName("@spec:AC-558 Token do investidor leva ROLE_INVESTIDOR e o do ADMIN leva ROLE_ADMIN")
    void tokenLevaOPapelReal() throws Exception {
        String emailInvestidor = emailUnico();
        cadastrar(emailInvestidor);
        String emailAdmin = emailUnico();
        usuarioRepository.save(new Usuario(null, emailAdmin, passwordEncoder.encode("senhaAdmin1"), Papel.ADMIN, SituacaoUsuario.ATIVO));

        assertThat(authoritiesDoLogin(emailInvestidor, "senha123")).containsExactly("ROLE_INVESTIDOR");
        assertThat(authoritiesDoLogin(emailAdmin, "senhaAdmin1")).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("@spec:AC-568 Autocadastro com papel ADMIN no corpo cria INVESTIDOR, e nenhuma entrada da API aceita papel")
    void ninguemSePromoveAAdmin() throws Exception {
        String email = emailUnico();
        String corpo = """
                {"nome": "Tentou Ser Admin", "email": "%s", "cpf": "%s", "senha": "senha123", "papel": "ADMIN"}
                """.formatted(email, cpfUnico());
        String json = mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        assertThat(usuarioRepository.findById(id).orElseThrow().getPapel()).isEqualTo(Papel.INVESTIDOR);
        assertThat(entradasComPapel()).isEmpty();
    }

    private List<String> authoritiesDoLogin(String email, String senha) throws Exception {
        String json = login(email, senha).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = objectMapper.readValue(json, AuthTokenResponseDTO.class).getToken();
        return jwtDecoder.decode(token).getClaimAsStringList("authorities");
    }

    // varre todo @RequestBody e parâmetro dos controllers atrás de algo chamado "papel";
    // spec-29: parâmetro de rota GET é filtro de leitura (ex.: ?papel= na listagem do ADMIN) e não atribui papel
    private static List<String> entradasComPapel() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> achados = new ArrayList<>();
        for (var bean : scanner.findCandidateComponents("com.apiexternabackend")) {
            for (Method metodo : Class.forName(bean.getBeanClassName()).getDeclaredMethods()) {
                for (Parameter parametro : metodo.getParameters()) {
                    if (parametro.getName().equalsIgnoreCase("papel") && !metodo.isAnnotationPresent(GetMapping.class)) {
                        achados.add(metodo + " -> " + parametro.getName());
                    }
                    if (!parametro.isAnnotationPresent(RequestBody.class)) continue;
                    for (Field campo : parametro.getType().getDeclaredFields()) {
                        if (campo.getName().equalsIgnoreCase("papel")) achados.add(parametro.getType().getName() + ".papel");
                    }
                }
            }
        }
        return achados;
    }

    private Long cadastrar(String email) throws Exception {
        String json = mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthCadastroRequestDTO("Investidor Teste", email, cpfUnico(), "senha123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO(email, senha))));
    }

    private static String emailUnico() {
        return "ident" + System.nanoTime() + "@email.com";
    }

    private static String cpfUnico() {
        return String.format("%011d", System.nanoTime() % 100_000_000_000L);
    }
}
