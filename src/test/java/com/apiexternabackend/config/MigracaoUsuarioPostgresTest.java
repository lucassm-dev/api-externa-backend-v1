package com.apiexternabackend.config;

import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.services.AutenticacaoService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec-15: a V17 roda num Postgres real sobre dados gravados no schema da V16.
 * O banco é levado à V16 e populado antes do contexto subir; o Flyway do próprio
 * Spring aplica a V17 na inicialização, como no deploy.
 */
@SpringBootTest
@ActiveProfiles("test")
class MigracaoUsuarioPostgresTest {

    private static final long ID_LEGADO = 42L;
    private static final String EMAIL_LEGADO = "legado@email.com";

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("16")
                .load()
                .migrate();
        new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()))
                .update("INSERT INTO investidor (id, nome, email, cpf, senha, ativo) VALUES (?, ?, ?, ?, ?, true)",
                        ID_LEGADO, "Investidor Legado", EMAIL_LEGADO, "11122233344",
                        new BCryptPasswordEncoder().encode("senhaAntiga1"));
    }

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired private AutenticacaoService autenticacaoService;
    @Autowired private JwtDecoder jwtDecoder;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("@spec:AC-551 Investidor cadastrado antes da V17 faz login com a mesma senha e mantém o id")
    void investidorLegadoContinuaLogando() {
        String token = autenticacaoService.login(new AuthLoginRequestDTO(EMAIL_LEGADO, "senhaAntiga1")).getToken();

        assertThat(jwtDecoder.decode(token).getSubject()).isEqualTo(String.valueOf(ID_LEGADO));
    }

    @Test
    @DisplayName("@spec:AC-556 O banco só aceita e-mail repetido quando o anterior está EXCLUIDO")
    void emailRepetidoSoComAnteriorExcluido() {
        String email = "dup@email.com";
        String insert = "INSERT INTO usuario (email, senha, papel, situacao) VALUES (?, 'hash', 'INVESTIDOR', 'ATIVO')";
        jdbc.update(insert, email);

        assertThatThrownBy(() -> jdbc.update(insert, email)).isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("UPDATE usuario SET situacao = 'EXCLUIDO' WHERE email = ?", email);
        jdbc.update(insert, email);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = ?", Integer.class, email)).isEqualTo(2);
    }
}
