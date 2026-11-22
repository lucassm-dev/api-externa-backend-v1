package com.apiexternabackend.config;

import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.dtos.ConfirmarTrocaEmailRequestDTO;
import com.apiexternabackend.domains.dtos.PedirTrocaEmailRequestDTO;
import com.apiexternabackend.domains.dtos.RedefinirSenhaRequestDTO;
import com.apiexternabackend.domains.CodigoRecuperacao;
import com.apiexternabackend.repositories.CodigoRecuperacaoRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-24 (#28): trocar e-mail com confirmação por código. SMTP substituído por mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrocaEmailIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CodigoRecuperacaoRepository codigoRepository;
    @MockBean private JavaMailSender mailSender;

    @Test
    @DisplayName("@spec:AC-625 Código vai para o novo e-mail e a confirmação troca o e-mail de acesso")
    void trocaComConfirmacao() throws Exception {
        String antigo = cadastrar();
        String novo = emailUnico();
        String token = token(antigo);

        String codigo = pedirCodigo(token, novo, "senha123");
        confirmar(token, codigo).andExpect(status().isNoContent());

        login(antigo).andExpect(status().isUnauthorized());
        login(novo).andExpect(status().isOk());
        me(token).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(novo));
    }

    @Test
    @DisplayName("@spec:AC-626 Sem confirmação o e-mail antigo continua valendo")
    void semConfirmacaoNadaMuda() throws Exception {
        String antigo = cadastrar();
        String novo = emailUnico();
        pedirCodigo(token(antigo), novo, "senha123");

        login(antigo).andExpect(status().isOk());
        login(novo).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("@spec:AC-627 Novo e-mail já usado por outro usuário é recusado no pedido e na confirmação")
    void emailEmUsoRecusado() throws Exception {
        String a = cadastrar();
        String b = cadastrar();
        pedir(token(a), b, "senha123").andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("AUT-001"));

        String livre = emailUnico();
        String tokenA = token(a);
        String codigo = pedirCodigo(tokenA, livre, "senha123");
        cadastrarCom(livre);
        confirmar(tokenA, codigo).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("AUT-001"));
        login(a).andExpect(status().isOk());
    }

    @Test
    @DisplayName("@spec:AC-628 Senha atual errada é recusada e nenhum e-mail sai")
    void senhaAtualErrada() throws Exception {
        String email = cadastrar();
        pedir(token(email), emailUnico(), "errada123")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("AUT-014"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("@spec:AC-629 Código expirado, errado além do limite ou já usado é recusado")
    void regrasDoCodigo() throws Exception {
        String email = cadastrar();
        String token = token(email);

        String codigo = pedirCodigo(token, emailUnico(), "senha123");
        String errado = codigo.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            confirmar(token, errado).andExpect(jsonPath("$.codigo").value("AUT-013"));
        }
        confirmar(token, codigo).andExpect(jsonPath("$.codigo").value("AUT-013"));

        codigo = pedirCodigo(token, emailUnico(), "senha123");
        CodigoRecuperacao c = codigoRepository.findAll().stream()
                .filter(x -> x.getUsuario().getEmail().equals(email) && x.getUsadoEm() == null).findFirst().orElseThrow();
        c.setExpiraEm(Instant.now().minusSeconds(1));
        codigoRepository.save(c);
        confirmar(token, codigo).andExpect(jsonPath("$.codigo").value("AUT-013"));

        String terceiroEmail = emailUnico();
        codigo = pedirCodigo(token, terceiroEmail, "senha123");
        confirmar(token, codigo).andExpect(status().isNoContent());
        confirmar(token, codigo).andExpect(jsonPath("$.codigo").value("AUT-013"));
    }

    @Test
    @DisplayName("@spec:AC-630 Falha no envio responde 503 EXT-012 e não grava código")
    void falhaNoEnvio() throws Exception {
        String email = cadastrar();
        doThrow(new MailSendException("fora")).when(mailSender).send(any(SimpleMailMessage.class));

        pedir(token(email), emailUnico(), "senha123")
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("EXT-012"));
        assertThat(codigoRepository.findAll()).noneMatch(c -> c.getUsuario().getEmail().equals(email));
    }

    @Test
    @DisplayName("@spec:AC-631 Quarto pedido em 15 minutos é recusado com AUT-015")
    void limiteDePedidos() throws Exception {
        String token = token(cadastrar());
        for (int i = 0; i < 3; i++) {
            pedir(token, emailUnico(), "senha123").andExpect(status().isAccepted());
        }
        pedir(token, emailUnico(), "senha123").andExpect(jsonPath("$.codigo").value("AUT-015"));
    }

    @Test
    @DisplayName("@spec:AC-632 Código de troca de e-mail não redefine senha")
    void codigosNaoSeMisturam() throws Exception {
        String email = cadastrar();
        String codigo = pedirCodigo(token(email), emailUnico(), "senha123");
        mockMvc.perform(post("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedefinirSenhaRequestDTO(email, codigo, "novaSenha9"))))
                .andExpect(jsonPath("$.codigo").value("AUT-013"));
    }

    private String pedirCodigo(String token, String novoEmail, String senha) throws Exception {
        clearInvocations(mailSender);
        pedir(token, novoEmail, senha).andExpect(status().isAccepted());
        ArgumentCaptor<SimpleMailMessage> msg = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msg.capture());
        assertThat(msg.getValue().getTo()).containsExactly(novoEmail);
        Matcher m = Pattern.compile("\\d{6}").matcher(msg.getValue().getText());
        assertThat(m.find()).isTrue();
        return m.group();
    }

    private ResultActions pedir(String token, String novoEmail, String senha) throws Exception {
        return mockMvc.perform(post("/auth/email").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PedirTrocaEmailRequestDTO(novoEmail, senha))));
    }

    private ResultActions confirmar(String token, String codigo) throws Exception {
        return mockMvc.perform(post("/auth/email/confirmar").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmarTrocaEmailRequestDTO(codigo))));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthLoginRequestDTO(email, "senha123"))));
    }

    private String token(String email) throws Exception {
        String json = login(email).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, AuthTokenResponseDTO.class).getToken();
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token));
    }

    private String cadastrar() throws Exception {
        return cadastrarCom(emailUnico());
    }

    private String cadastrarCom(String email) throws Exception {
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthCadastroRequestDTO("Troca", email,
                                String.format("%011d", System.nanoTime() % 100_000_000_000L), "senha123"))))
                .andExpect(status().isCreated());
        return email;
    }

    private static String emailUnico() {
        return "email" + System.nanoTime() + "@email.com";
    }
}
