package com.apiexternabackend.deploy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeployScriptTest {

    private static final String SCRIPT = "deploy.sh";

    private ScriptSandbox sandbox;
    private Path frontendDir;

    @BeforeEach
    void setUp() {
        sandbox = ScriptSandbox.criar(SCRIPT);
        frontendDir = sandbox.criarFrontendRepo();
    }

    private Map<String, String> envPadrao() {
        Map<String, String> env = new HashMap<>();
        env.put("FRONTEND_DIR", frontendDir.toAbsolutePath().toString());
        env.put("HEALTH_TIMEOUT", "5");
        env.put("HEALTH_INTERVAL", "1");
        env.put("FRONTEND_PORT", "8081");
        return env;
    }

    @Test
    @DisplayName("@spec:AC-526 Sem .env o deploy nem começa")
    void semEnvNaoComeca() {
        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isNotZero();
        assertThat(resultado.stderr()).containsIgnoringCase(".env");
        assertThat(sandbox.chamadas()).isEmpty();
    }

    @Test
    @DisplayName("@spec:AC-527 Mudança não commitada no backend aborta o deploy")
    void backendSujoAborta() {
        sandbox.criarEnv();
        sandbox.marcarSujo(sandbox.backendDir());

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isNotZero();
        assertThat(resultado.stderr()).containsIgnoringCase("backend");
        assertThat(sandbox.chamadas()).noneMatch(linha -> linha.startsWith("git") && linha.contains("reset"));
    }

    @Test
    @DisplayName("@spec:AC-527 Mudança não commitada no frontend aborta o deploy")
    void frontendSujoAborta() {
        sandbox.criarEnv();
        sandbox.marcarSujo(frontendDir);

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isNotZero();
        assertThat(resultado.stderr()).containsIgnoringCase("frontend");
        assertThat(sandbox.chamadas()).noneMatch(linha -> linha.startsWith("git") && linha.contains("reset"));
    }

    @Test
    @DisplayName("@spec:AC-528 Docker Compose antigo aborta o deploy")
    void composeAntigoAborta() {
        sandbox.criarEnv();
        sandbox.definirVersaoCompose("Docker Compose version v2.20.0");

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isNotZero();
        assertThat(resultado.stderr()).contains("2.24");
    }

    @Test
    @DisplayName("@spec:AC-529 Caminho feliz atualiza e sobe com o override")
    void caminhoFelizFuncionaComOverride() {
        sandbox.criarEnv();
        sandbox.definirCommitParaBranch(sandbox.backendDir(), "main", "aaa1111");
        sandbox.definirCommitParaBranch(frontendDir, "main", "bbb2222");

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isZero();
        assertThat(resultado.stdout()).contains("aaa1111");
        assertThat(resultado.stdout()).contains("bbb2222");
        assertThat(sandbox.chamadas())
                .anyMatch(linha -> linha.contains("compose") && linha.contains("-f")
                        && linha.contains("docker-compose.yml") && linha.contains("docker-compose.prod.yml")
                        && linha.contains("up") && linha.contains("--build"));
        assertThat(sandbox.chamadas()).filteredOn(linha -> linha.startsWith("git") && linha.contains("reset"))
                .hasSize(2);
    }

    @Test
    @DisplayName("@spec:AC-530 Branch escolhida por variável")
    void branchEscolhidaPorVariavel() {
        sandbox.criarEnv();
        sandbox.definirCommitParaBranch(sandbox.backendDir(), "develop", "ccc3333");
        sandbox.definirCommitParaBranch(frontendDir, "develop", "ddd4444");

        Map<String, String> env = envPadrao();
        env.put("BRANCH", "develop");

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, env);

        assertThat(resultado.codigoSaida()).isZero();
        assertThat(resultado.stdout()).contains("ccc3333");
        assertThat(resultado.stdout()).contains("ddd4444");
        assertThat(sandbox.chamadas()).anyMatch(linha -> linha.contains("fetch origin develop"));
        assertThat(sandbox.chamadas()).anyMatch(linha -> linha.contains("reset --hard origin/develop"));
    }

    @Test
    @DisplayName("@spec:AC-531 Serviço que não fica saudável falha o deploy")
    void servicoDoenteFalha() {
        sandbox.criarEnv();
        sandbox.definirSaude("investimentos-api", "starting");

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isNotZero();
        assertThat(resultado.stderr()).contains("investimentos-api");
        assertThat(resultado.stderr()).contains("log fake de investimentos-api");
    }

    @Test
    @DisplayName("@spec:AC-532 /saude sem 200 pelo nginx falha o deploy")
    void saudeSem200Falha() {
        sandbox.criarEnv();
        sandbox.definirCodigoSaude("500");

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isNotZero();
    }

    @Test
    @DisplayName("@spec:AC-532 rota da API respondendo 502 pelo nginx falha o deploy")
    void rotaApi502Falha() {
        sandbox.criarEnv();
        sandbox.definirCodigoApi("502");

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isNotZero();
    }

    @Test
    @DisplayName("@spec:AC-533 Funciona chamado de qualquer diretório pelo caminho absoluto")
    void funcionaDeQualquerDiretorio() throws IOException {
        sandbox.criarEnv();
        sandbox.definirCommitParaBranch(sandbox.backendDir(), "main", "eee5555");
        sandbox.definirCommitParaBranch(frontendDir, "main", "fff6666");
        Path fora = Files.createTempDirectory("fora-do-repo-");

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao(), fora);

        assertThat(resultado.codigoSaida()).isZero();
        assertThat(resultado.stdout()).contains("eee5555");
        assertThat(resultado.stdout()).contains("fff6666");
    }
}
