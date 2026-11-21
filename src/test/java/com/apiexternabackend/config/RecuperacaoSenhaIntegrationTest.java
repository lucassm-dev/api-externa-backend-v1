package com.apiexternabackend.config;

import com.apiexternabackend.domains.CodigoRecuperacao;
import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.dtos.EsqueciSenhaRequestDTO;
import com.apiexternabackend.domains.dtos.RedefinirSenhaRequestDTO;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.CodigoRecuperacaoRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #26: esqueci minha senha com código por e-mail. SMTP substituído por mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecuperacaoSenhaIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CodigoRecuperacaoRepository codigoRepository;
    @MockBean private JavaMailSender mailSender;

    @Test
    @DisplayName("@spec:AC-609 Senha antiga para de funcionar e a nova funciona")
    void trocaASenha() throws Exception {
        String email = cadastrar();
        redefinir(email, pedirCodigo(email), "novaSenha9").andExpect(status().isNoContent());

        login(email, "senha123").andExpect(status().isUnauthorized());
        login(email, "novaSenha9").andExpect(status().isOk());
    }

    @Test
    @DisplayName("@spec:AC-610 Token emitido antes da troca recebe 401")
    void tokenAntigoCai() throws Exception {
        String email = cadastrar();
        String tokenAntigo = token(login(email, "senha123"));
        Thread.sleep(1100); // iat tem precisão de segundos
        redefinir(email, pedirCodigo(email), "novaSenha9").andExpect(status().isNoContent());

        me(tokenAntigo).andExpect(status().isUnauthorized());
        me(token(login(email, "novaSenha9"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("@spec:AC-611 Código já usado é recusado")
    void codigoUsadoRecusado() throws Exception {
        String email = cadastrar();
        String codigo = pedirCodigo(email);
        redefinir(email, codigo, "novaSenha9").andExpect(status().isNoContent());
        redefinir(email, codigo, "outraSenha9").andExpect(jsonPath("$.codigo").value("AUT-013"));
    }

    @Test
    @DisplayName("@spec:AC-612 Código expirado é recusado")
    void codigoExpiradoRecusado() throws Exception {
        String email = cadastrar();
        String codigo = pedirCodigo(email);
        CodigoRecuperacao c = codigoRepository.findAll().stream()
                .filter(x -> x.getUsuario().getEmail().equals(email)).findFirst().orElseThrow();
        c.setExpiraEm(Instant.now().minusSeconds(1));
        codigoRepository.save(c);

        redefinir(email, codigo, "novaSenha9")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AUT-013"));
    }

    @Test
    @DisplayName("@spec:AC-613 Após 5 erros, nem o código certo vale")
    void limiteDeTentativas() throws Exception {
        String email = cadastrar();
        String codigo = pedirCodigo(email);
        String errado = codigo.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            redefinir(email, errado, "novaSenha9").andExpect(jsonPath("$.codigo").value("AUT-013"));
        }
        redefinir(email, codigo, "novaSenha9").andExpect(jsonPath("$.codigo").value("AUT-013"));
    }

    @Test
    @DisplayName("@spec:AC-614 Pedir novo código invalida o anterior")
    void novoCodigoInvalidaAnterior() throws Exception {
        String email = cadastrar();
        String primeiro = pedirCodigo(email);
        String segundo = pedirCodigo(email);
        if (!primeiro.equals(segundo)) {
            redefinir(email, primeiro, "novaSenha9").andExpect(jsonPath("$.codigo").value("AUT-013"));
        }
        redefinir(email, segundo, "novaSenha9").andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("@spec:AC-615 E-mail inexistente responde igual ao existente e não envia nada")
    void emailInexistenteRespondeIgual() throws Exception {
        String existente = cadastrar();
        String a = esqueci(existente).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String b = esqueci("ninguem" + System.nanoTime() + "@email.com")
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertThat(a).isEqualTo(b);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("@spec:AC-616 Usuário bloqueado não recebe código nem redefine")
    void bloqueadoNaoRecupera() throws Exception {
        String email = cadastrar();
        var u = usuarioRepository.findByEmailAndSituacaoNot(email, SituacaoUsuario.EXCLUIDO).orElseThrow();
        u.setSituacao(SituacaoUsuario.BLOQUEADO);
        usuarioRepository.save(u);

        esqueci(email).andExpect(status().isAccepted());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        redefinir(email, "123456", "novaSenha9").andExpect(jsonPath("$.codigo").value("AUT-013"));
    }

    @Test
    @DisplayName("@spec:AC-617 Mais de 3 pedidos em 15 minutos não enviam e-mail")
    void limiteDePedidos() throws Exception {
        String email = cadastrar();
        for (int i = 0; i < 4; i++) {
            esqueci(email).andExpect(status().isAccepted());
        }
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("@spec:AC-618 Falha no SMTP responde igual e não grava código")
    void falhaNoEnvioRespondeIgual() throws Exception {
        String email = cadastrar();
        doThrow(new MailSendException("fora")).when(mailSender).send(any(SimpleMailMessage.class));

        esqueci(email).andExpect(status().isAccepted()).andExpect(content().string(""));
        assertThat(codigoRepository.findAll()).noneMatch(c -> c.getUsuario().getEmail().equals(email));
    }

    @Test
    @DisplayName("@spec:AC-619 Código é guardado como hash")
    void codigoGuardadoComoHash() throws Exception {
        String email = cadastrar();
        String codigo = pedirCodigo(email);
        assertThat(codigoRepository.findAll()).filteredOn(c -> c.getUsuario().getEmail().equals(email))
                .allMatch(c -> !c.getCodigoHash().contains(codigo));
    }

    private String pedirCodigo(String email) throws Exception {
        clearInvocations(mailSender);
        esqueci(email).andExpect(status().isAccepted());
        ArgumentCaptor<SimpleMailMessage> msg = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msg.capture());
        Matcher m = Pattern.compile("\\d{6}").matcher(msg.getValue().getText());
        assertThat(m.find()).isTrue();
        return m.group();
    }

    private ResultActions esqueci(String email) throws Exception {
        return mockMvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EsqueciSenhaRequestDTO(email))));
    }

    private ResultActions redefinir(String email, String codigo, String senha) throws Exception {
        return mockMvc.perform(post("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RedefinirSenhaRequestDTO(email, codigo, senha))));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO(email, senha))));
    }

    private String token(ResultActions login) throws Exception {
        String json = login.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, AuthTokenResponseDTO.class).getToken();
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token));
    }

    private String cadastrar() throws Exception {
        String email = "rec" + System.nanoTime() + "@email.com";
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthCadastroRequestDTO("Rec", email,
                                String.format("%011d", System.nanoTime() % 100_000_000_000L), "senha123"))))
                .andExpect(status().isCreated());
        return email;
    }
}
