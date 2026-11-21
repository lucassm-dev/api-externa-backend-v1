package com.apiexternabackend.config;

import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spec-18: o bloqueio vale na requisição seguinte, mesmo com token ainda válido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BloqueioImediatoIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("@spec:AC-585 Bloqueio vale na requisição seguinte")
    void bloqueioValeNaRequisicaoSeguinte() throws Exception {
        String tokenAdmin = token(usuario(Papel.ADMIN));
        Usuario investidor = usuario(Papel.INVESTIDOR);
        String tokenInvestidor = token(investidor);
        me(tokenInvestidor).andExpect(status().isOk());

        mockMvc.perform(patch("/admin/usuarios/" + investidor.getId() + "/bloquear")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"uso abusivo\"}"))
                .andExpect(status().isOk());

        me(tokenInvestidor)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-010"));

        mockMvc.perform(patch("/admin/usuarios/" + investidor.getId() + "/desbloquear")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        me(tokenInvestidor).andExpect(status().isOk());
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token));
    }

    private Usuario usuario(Papel papel) {
        return usuarioRepository.save(new Usuario(null, "bloq" + System.nanoTime() + "@email.com", "hash", papel, SituacaoUsuario.ATIVO));
    }

    private String token(Usuario usuario) {
        return jwtService.gerar(usuario.getId(), usuario.getPapel()).getTokenValue();
    }
}
